/**
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at

 *  http://www.apache.org/licenses/LICENSE-2.0

 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.stratos.cli;

import java.net.ConnectException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.axis2.transport.http.HttpTransportProperties;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.stratos.cli.beans.SubscriptionInfo;
import org.apache.stratos.cli.beans.TenantInfoBean;
import org.apache.stratos.cli.beans.UserInfoBean;
import org.apache.stratos.cli.beans.autoscaler.partition.Partition;
import org.apache.stratos.cli.beans.autoscaler.policy.autoscale.AutoscalePolicy;
import org.apache.stratos.cli.beans.autoscaler.policy.deployment.DeploymentPolicy;
import org.apache.stratos.cli.beans.cartridge.Cartridge;
import org.apache.stratos.cli.beans.cartridge.CartridgeInfoBean;
import org.apache.stratos.cli.beans.cartridge.PortMapping;
import org.apache.stratos.cli.beans.cartridge.ServiceDefinitionBean;
import org.apache.stratos.cli.beans.kubernetes.KubernetesGroup;
import org.apache.stratos.cli.beans.kubernetes.KubernetesGroupList;
import org.apache.stratos.cli.beans.kubernetes.KubernetesHost;
import org.apache.stratos.cli.beans.kubernetes.KubernetesHostList;
import org.apache.stratos.cli.beans.topology.Cluster;
import org.apache.stratos.cli.beans.topology.Member;
import org.apache.stratos.cli.exception.CommandException;
import org.apache.stratos.cli.exception.ExceptionMapper;
import org.apache.stratos.cli.utils.CliConstants;
import org.apache.stratos.cli.utils.CliUtils;
import org.apache.stratos.cli.utils.RowMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class RestCommandLineService {

    private static final Logger logger = LoggerFactory.getLogger(RestCommandLineService.class);

    private RestClient restClient;

    // REST endpoints
    private final String INIT_COOKIE_ENDPOINT = "/stratos/admin/cookie";

    private final String SUBSCRIBE_CARTRIDGE_ENDPOINT = "/stratos/admin/cartridge/subscribe";
    private final String ADD_TENANT_ENDPOINT = "/stratos/admin/tenant";
    private final String ADD_USER_ENDPOINT = "/stratos/admin/user";

    private final String UNSUBSCRIBE_CARTRIDGE_OF_TENANT_ENDPOINT = "/stratos/admin/cartridge/unsubscribe";
    private final String SYNCHRONIZE_ARTIFACTS_ENDPOINT = "/stratos/admin/cartridge/sync";

    private final String DEPLOY_CARTRIDGE_ENDPOINT = "/stratos/admin/cartridge/definition";
    private final String DEPLOY_PARTITION_ENDPOINT = "/stratos/admin/policy/deployment/partition";
    private final String DEPLOY_AUTOSCALING_POLICY_ENDPOINT = "/stratos/admin/policy/autoscale";
    private final String DEPLOY_DEPLOYMENT_POLICY_ENDPOINT = "/stratos/admin/policy/deployment";
    private final String DEPLOY_SERVICE_ENDPOINT = "/stratos/admin/service/definition";
    private final String DEPLOY_KUBERNETES_GROUP_ENDPOINT = "/stratos/admin/kubernetes/deploy/group";
    private final String DEPLOY_KUBERNETES_HOST_ENDPOINT = "/stratos/admin/kubernetes/deploy/host";

    private final String LIST_PARTITION_ENDPOINT = "/stratos/admin/partition";
    private final String LIST_AUTOSCALING_POLICY_ENDPOINT = "/stratos/admin/policy/autoscale";
    private final String LIST_DEPLOYMENT_POLICY_ENDPOINT = "/stratos/admin/policy/deployment";
    private final String LIST_CARTRIDGES_ENDPOINT = "/stratos/admin/cartridge/available/list";
    private final String LIST_CARTRIDGE_SUBSCRIPTIONS_ENDPOINT = "/stratos/admin/cartridge/list/subscribed";
    private final String LIST_SERVICE_ENDPOINT = "/stratos/admin/service";
    private final String LIST_TENANT_ENDPOINT = "/stratos/admin/tenant/list";
    private final String LIST_USERS_ENDPOINT = "/stratos/admin/user/list";
    private final String LIST_KUBERNETES_GROUP_ENDPOINT = "/stratos/admin/kubernetes/group";
    private final String LIST_KUBERNETES_HOSTS_ENDPOINT = "/stratos/admin/kubernetes/hosts/{groupId}";

    private final String GET_CARTRIDGE_ENDPOINT = "/stratos/admin/cartridge/available/info";
    private final String GET_CARTRIDGE_OF_TENANT_ENDPOINT = "/stratos/admin/cartridge/info/{id}";
    private final String GET_CLUSTER_OF_TENANT_ENDPOINT = "/stratos/admin/cluster/";
    private final String GET_KUBERNETES_GROUP_ENDPOINT = "/stratos/admin/kubernetes/group/{id}";
    private final String GET_KUBERNETES_MASTER_ENDPOINT = "/stratos/admin/kubernetes/master/{id}";
    private final String GET_KUBERNETES_HOST_ENDPOINT = "/stratos/admin/kubernetes/hosts/{id}";

    private final String DEACTIVATE_TENANT_ENDPOINT = "/stratos/admin/tenant/deactivate";
    private final String ACTIVATE_TENANT_ENDPOINT = "/stratos/admin/tenant/activate";

    private final String UNDEPLOY_KUBERNETES_GROUP_ENDPOINT = "/stratos/admin/kubernetes/group/{id}";
    private final String UNDEPLOY_KUBERNETES_HOST_ENDPOINT = "/stratos/admin/kubernetes/host/{id}";

    private final String UPDATE_KUBERNETES_MASTER_ENDPOINT = "/stratos/admin/kubernetes/update/master";
    private final String UPDATE_KUBERNETES_HOST_ENDPOINT = "/stratos/admin/kubernetes/update/host";

    private static class SingletonHolder {
        private final static RestCommandLineService INSTANCE = new RestCommandLineService();
    }

    public static RestCommandLineService getInstance() {
        return SingletonHolder.INSTANCE;
    }

    public Gson getGson() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        return gsonBuilder.create();
    }

    // Loing method. This will authenticate the user
    public boolean login(String serverURL, String username, String password, boolean validateLogin) throws Exception {
        try {
            // Following code will avoid validating certificate
            SSLContext sc;
            // Get SSL context
            sc = SSLContext.getInstance("SSL");
            // Create empty HostnameVerifier
            HostnameVerifier hv = new HostnameVerifier() {
                public boolean verify(String urlHostName, SSLSession session) {
                    return true;
                }
            };
            // Create a trust manager that does not validate certificate
            // chains
            TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                }

                public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                }
            }};
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            SSLContext.setDefault(sc);
            HttpsURLConnection.setDefaultHostnameVerifier(hv);
        } catch (Exception e) {
            throw new RuntimeException("Error while authentication process!", e);
        }

        // Initialized client
        try {
            initializeRestClient(serverURL, username, password);

            if (logger.isDebugEnabled()) {
                logger.debug("Initialized REST Client for user {}", username);
            }
        } catch (AxisFault e) {
            System.out.println("Error connecting to the back-end");
            throw new CommandException(e);
        }

        DefaultHttpClient httpClient = new DefaultHttpClient();
        try {
            if (validateLogin) {
                HttpResponse response = restClient.doGet(httpClient, restClient.getBaseURL() + INIT_COOKIE_ENDPOINT);

                if (response != null) {
                    String responseCode = "" + response.getStatusLine().getStatusCode();
                    if ((responseCode.equals(CliConstants.RESPONSE_OK)) && (response.toString().contains("WWW-Authenticate: Basic"))) {
                        return true;
                    } else {
                        System.out.println("Invalid STRATOS_URL");
                    }
                }
                return false;
            } else {
                // Just return true as we don't need to validate
                return true;
            }
        } catch (ClientProtocolException e) {
            System.out.println("Authentication failed!");
            return false;
        } catch (ConnectException e) {
            System.out.println("Could not connect to stratos manager");
            return false;
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }

    // Initialize the rest client and set username and password of the user
    private void initializeRestClient(String serverURL, String username, String password) throws AxisFault {
        HttpTransportProperties.Authenticator authenticator = new HttpTransportProperties.Authenticator();
        authenticator.setUsername(username);
        authenticator.setPassword(password);
        authenticator.setPreemptiveAuthentication(true);

        ConfigurationContext configurationContext = null;
        try {
            configurationContext = ConfigurationContextFactory.createDefaultConfigurationContext();
        } catch (Exception e) {
            String msg = "Backend error occurred. Please contact the service admins!";
            throw new AxisFault(msg, e);
        }
        HashMap<String, TransportOutDescription> transportsOut = configurationContext
                .getAxisConfiguration().getTransportsOut();
        for (TransportOutDescription transportOutDescription : transportsOut.values()) {
            transportOutDescription.getSender().init(configurationContext, transportOutDescription);
        }

        this.restClient = new RestClient(serverURL, username, password);
    }

    public Cartridge getCartridge(String cartridgeType) throws CommandException {
        try {
            CartridgeList list = (CartridgeList) restClient.listEntity(LIST_CARTRIDGES_ENDPOINT,
                    CartridgeList.class, "cartridges");

            for (int i = 0; i < list.getCartridge().size(); i++) {
                Cartridge cartridge = list.getCartridge().get(i);
                if (cartridgeType.equals(cartridge.getCartridgeType())) {
                    return cartridge;
                }
            }
        } catch (Exception e) {
            String message = "Error in getting cartridge";
            System.out.println(message);
            logger.error(message, e);
        }
        return null;
    }

    public ArrayList<Cartridge> listCartridgesByServiceGroup(String serviceGroup) throws CommandException {
        try {
            CartridgeList list = (CartridgeList) restClient.listEntity(LIST_CARTRIDGES_ENDPOINT,
                    CartridgeList.class, "cartridges");

            ArrayList<Cartridge> arrayList = new ArrayList<Cartridge>();
            for (int i = 0; i < list.getCartridge().size(); i++) {
                if (serviceGroup.equals(list.getCartridge().get(i).getServiceGroup())) {
                    arrayList.add(list.getCartridge().get(i));
                }
            }

            return arrayList;

        } catch (Exception e) {
            String message = "Error in listing cartridges";
            System.out.println(message);
            logger.error(message, e);
            return null;
        }
    }

    // List currently available multi tenant and single tenant cartridges
    public void listCartridges() throws CommandException {
        try {
            CartridgeList cartridgeList = (CartridgeList) restClient.listEntity(LIST_CARTRIDGES_ENDPOINT,
                    CartridgeList.class, "cartridges");

            if ((cartridgeList == null) || (cartridgeList.getCartridge() == null) ||
                    (cartridgeList.getCartridge().size() == 0)) {
                System.out.println("No cartridges found");
                return;
            }

            RowMapper<Cartridge> cartridgeMapper = new RowMapper<Cartridge>() {

                public String[] getData(Cartridge cartridge) {
                    String[] data = new String[6];
                    data[0] = cartridge.getCartridgeType();
                    data[1] = cartridge.getDisplayName();
                    data[2] = cartridge.getDescription();
                    data[3] = cartridge.getVersion();
                    data[4] = String.valueOf(cartridge.isMultiTenant());
                    data[5] = cartridge.getIsPublic() ? "Public" : "Private";
                    return data;
                }
            };

            Cartridge[] cartridges = new Cartridge[cartridgeList.getCartridge().size()];
            cartridges = cartridgeList.getCartridge().toArray(cartridges);

            System.out.println("Cartridges found:");
            CliUtils.printTable(cartridges, cartridgeMapper, "Type", "Name", "Description", "Version",
                    "Is Multi-Tenant", "Accessibility");
        } catch (Exception e) {
            String message = "Error in listing cartridges";
            System.out.println(message);
            logger.error(message, e);
        }
    }

    // Describe currently available multi tenant and single tenant cartridges
    public void describeAvailableCartridges(String cartridgeType) throws CommandException {
        try {
            CartridgeList cartridgeList = (CartridgeList) restClient.listEntity(LIST_CARTRIDGES_ENDPOINT,
                    CartridgeList.class, "cartridges");

            if ((cartridgeList == null) || (cartridgeList.getCartridge() == null) ||
                    (cartridgeList.getCartridge().size() == 0)) {
                System.out.println("Cartridge not found");
                return;
            }

            for (Cartridge tmp : cartridgeList.getCartridge()) {
                if (tmp.getCartridgeType().equalsIgnoreCase(cartridgeType)) {
                    System.out.println("Cartridge:");
                    System.out.println(getGson().toJson(tmp));
                    return;
                }
            }
            System.out.println("Cartridge not found: " + cartridgeType);
        } catch (Exception e) {
            String message = "Error in describing cartridge: " + cartridgeType;
            System.out.println(message);
            logger.error(message, e);
        }
    }

    // List subscribe cartridges
    public void listCartridgeSubscriptions(final boolean showURLs) throws CommandException {
        try {
            CartridgeList cartridgeList = (CartridgeList) restClient.listEntity(LIST_CARTRIDGE_SUBSCRIPTIONS_ENDPOINT,
                    CartridgeList.class, "cartridge subscriptions");

            if ((cartridgeList == null) || (cartridgeList.getCartridge() == null) ||
                    (cartridgeList.getCartridge().size() == 0)) {
                System.out.println("No cartridge subscriptions found");
                return;
            }

            CartridgeList applicationCartridgeList = new CartridgeList();

            // Filter out LB cartridges
            List<Cartridge> allCartridges = cartridgeList.getCartridge();
            for (Cartridge cartridge : allCartridges) {
                if (!cartridge.isLoadBalancer()) {
                    applicationCartridgeList.getCartridge().add(cartridge);
                }
            }

            Cartridge[] cartridges = new Cartridge[applicationCartridgeList.getCartridge().size()];
            cartridges = applicationCartridgeList.getCartridge().toArray(cartridges);

            RowMapper<Cartridge> cartridgeMapper = new RowMapper<Cartridge>() {

                public String[] getData(Cartridge cartridge) {
                    String[] data = showURLs ? new String[11] : new String[9];
                    data[0] = cartridge.getCartridgeType();
                    data[1] = cartridge.getDisplayName();
                    data[2] = cartridge.getIsPublic() ? "Public" : "Private";
                    data[3] = cartridge.getVersion();
                    data[4] = cartridge.isMultiTenant() ? "Multi-Tenant" : "Single-Tenant";
                    data[5] = cartridge.getCartridgeAlias();
                    data[6] = cartridge.getStatus();
                    data[7] = cartridge.isMultiTenant() ? "N/A" : String.valueOf(cartridge.getActiveInstances());
                    data[8] = cartridge.getHostName();
                    if (showURLs) {
                        data[9] = getAccessURLs(cartridge);
                        data[10] = cartridge.getRepoURL() != null ? cartridge.getRepoURL() : "";
                    }
                    return data;

                }
            };

            List<String> headers = new ArrayList<String>();
            headers.add("Type");
            headers.add("Name");
            headers.add("Accessibility");
            headers.add("Version");
            headers.add("Tenancy Model");
            headers.add("Alias");
            headers.add("Status");
            headers.add("Running Instances");
            headers.add("Host Name");
            if (showURLs) {
                headers.add("Access URL(s)");
                headers.add("Repo URL");
            }

            System.out.println("Cartridge subscriptions found:");
            CliUtils.printTable(cartridges, cartridgeMapper, headers.toArray(new String[headers.size()]));
        } catch (Exception e) {
            String message = "Error in listing cartridge subscriptions";
            System.out.println(message);
            logger.error(message, e);
        }
    }

    // Lists subscribed cartridge info (from alias)
    public void describeCartridgeSubscription(String alias) throws CommandException {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        try {
            CartridgeWrapper cartridgeWrapper = (CartridgeWrapper) restClient.getEntity(GET_CARTRIDGE_OF_TENANT_ENDPOINT,
                    CartridgeWrapper.class, alias, "cartridge subscription");

            if((cartridgeWrapper == null) || (cartridgeWrapper.getCartridge() == null)) {
                System.out.println("Cartridge subscription not found: " + alias);
                return;
            }

            Cartridge cartridge = cartridgeWrapper.getCartridge();

            // Get LB IP s
            Map<String, Set<String>> lbIpMap = getLbIpList(cartridge, httpClient);
            final Set<String> lbPrivateIpSet = lbIpMap.get("private");
            final Set<String> lbFloatingIpSet = lbIpMap.get("floating");
            Cartridge[] cartridges = new Cartridge[1];
            cartridges[0] = cartridge;

            System.out.println("\nSubscribed Cartridges Info\n");
            System.out.println("\tType : " + cartridge.getCartridgeType());
            System.out.println("\tName : " + cartridge.getDisplayName());
            System.out.println("\tVersion : " + cartridge.getVersion());
            System.out.println("\tPublic : " + cartridge.getIsPublic());
            String tenancy = cartridge.isMultiTenant() ? "Multi-Tenant" : "Single-Tenant";
            System.out.println("\tTenancy Model	: " + tenancy);
            System.out.println("\tAlias : " + cartridge.getCartridgeAlias());
            System.out.println("\tStatus : " + cartridge.getStatus());
            String instanceCount = String.valueOf(cartridge.getActiveInstances());
            System.out.println("\tRunning Instances	: " + instanceCount);
            System.out.println("\tAccess URL(s) : " + getAccessURLs(cartridge));
            if (cartridge.getRepoURL() != null) {
                System.out.println("\tRepo URL : " + cartridge.getRepoURL());
            }
            System.out.println("\tLB Private IP	: " + lbPrivateIpSet.toString());
            if (lbFloatingIpSet != null) {
                System.out.println("\tLB Floating IP : " + lbFloatingIpSet.toString());
            }
            if (cartridge.getProvider().equals("data")) {
                System.out.println("\tDB-username : " + cartridge.getDbUserName());
                System.out.println("\tDB-password : " + cartridge.getPassword());
                System.out.println("\tDB-Host IP (private)  : " + cartridge.getIp());
                if (cartridge.getPublicIp() != null) {
                    System.out.println("\tDB-Host IP (floating) : "
                            + cartridge.getPublicIp());
                }
            }
        } catch (Exception e) {
            String message = "Error in getting cartridge subscription";
            System.out.println(message);
            logger.error(message, e);
        }
    }

    private Map<String, Set<String>> getLbIpList(Cartridge cartridge, DefaultHttpClient httpClient) throws Exception {
        try {
            Map<String, Set<String>> privateFloatingLBIPMap = new HashMap<String, Set<String>>();
            Set<String> lbFloatingIpSet = new HashSet<String>();
            Set<String> lbPrivateIpSet = new HashSet<String>();
            Member[] members = getMembers(cartridge.getCartridgeType(), cartridge.getCartridgeAlias(), httpClient);

            Set<String> lbClusterIdSet = new HashSet<String>();

            for (Member member : members) {
                lbClusterIdSet.add(member.getLbClusterId());
                cartridge.setIp(member.getMemberIp());
                cartridge.setPublicIp(member.getMemberPublicIp());
            }

            // Invoke  cluster/{clusterId}
            for (String clusterId : lbClusterIdSet) {
                HttpResponse responseCluster = restClient.doGet(httpClient, restClient.getBaseURL()
                        + GET_CLUSTER_OF_TENANT_ENDPOINT + "lb");

                String responseCode = "" + responseCluster.getStatusLine().getStatusCode();
                String resultStringCluster = CliUtils.getHttpResponseString(responseCluster);

                GsonBuilder gsonBuilder = new GsonBuilder();
                Gson gson = gsonBuilder.create();

                if (!responseCode.equals(CliConstants.RESPONSE_OK)) {
                    ExceptionMapper exception = gson.fromJson(resultStringCluster, ExceptionMapper.class);
                    System.out.println(exception);
                    return null;
                }

                ArrayList<Cluster> clusterList = getClusterListObjectFromString(resultStringCluster);
                Cluster cluster = clusterList.get(0);
                if (cluster == null) {
                    System.out.println("Subscribe cartridge list is null");
                    return null;
                }

                Member[] lbMembers = new Member[cluster.getMember().size()];
                lbMembers = cluster.getMember().toArray(lbMembers);

                for (Member lbMember : lbMembers) {
                    lbPrivateIpSet.add(lbMember.getMemberIp());
                    lbFloatingIpSet.add(lbMember.getMemberPublicIp());
                }

            }
            privateFloatingLBIPMap.put("private", lbPrivateIpSet);
            privateFloatingLBIPMap.put("floating", lbFloatingIpSet);

            return privateFloatingLBIPMap;
        } catch (Exception e) {
            String message = "Error in getting load balancer ip list";
            System.out.println(message);
            logger.error(message, e);
            return null;
        }
    }

    public void listMembersOfCluster(String cartridgeType, String alias) throws CommandException {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        try {

            Member[] members = getMembers(cartridgeType, alias, httpClient);

            if (members == null) {
                // these conditions are handled in the getMembers method
                return;
            }

            if (members.length == 0) {
                if (logger.isDebugEnabled()) {
                    logger.debug("No members found");
                }
                System.out.println("No members found for the corresponding cluster for type " + cartridgeType
                        + ", alias " + alias);
                return;
            }

            System.out.println("\nList of members in the [cluster]: " + alias);
            for (Member member : members) {
                System.out.println("\n\tServiceName : " + member.getServiceName());
                System.out.println("\tClusterId : " + member.getClusterId());
                System.out.println("\tNewtworkPartitionId : " + member.getNetworkPartitionId());
                System.out.println("\tPartitionId : " + member.getPartitionId());
                System.out.println("\tStatus : " + member.getStatus());
                if (member.getLbClusterId() != null) {
                    System.out.println("\tLBCluster : " + member.getLbClusterId());
                }
                System.out.println("\tMemberPrivateIp : " + member.getMemberIp());
                System.out.println("\tMemberFloatingIp : " + member.getMemberPublicIp());
                System.out.println("\t-----------------------");
            }

            System.out.println("==================================================");
            System.out.println("List of LB members for the [cluster]: " + alias);

            // Invoke  cluster/{clusterId}
            for (Member m : members) {
                HttpResponse responseCluster = restClient.doGet(httpClient, restClient.getBaseURL() + GET_CLUSTER_OF_TENANT_ENDPOINT
                        + "clusterId/" + m.getLbClusterId());

                String responseCode = "" + responseCluster.getStatusLine().getStatusCode();
                String resultStringCluster = CliUtils.getHttpResponseString(responseCluster);

                GsonBuilder gsonBuilder = new GsonBuilder();
                Gson gson = gsonBuilder.create();

                if (!responseCode.equals(CliConstants.RESPONSE_OK)) {
                    ExceptionMapper exception = gson.fromJson(resultStringCluster, ExceptionMapper.class);
                    System.out.println(exception);
                    break;
                }

                printLBs(resultStringCluster);
            }

        } catch (Exception e) {
            String message = "Error in listing members";
            System.out.println(message);
            logger.error(message, e);
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }

    private Member[] getMembers(String cartridgeType, String alias, DefaultHttpClient httpClient) throws Exception {
        try {
            HttpResponse response = restClient.doGet(httpClient, restClient.getBaseURL()
                    + GET_CLUSTER_OF_TENANT_ENDPOINT + cartridgeType + "/" + alias);

            String responseCode = "" + response.getStatusLine().getStatusCode();

            Gson gson = new Gson();
            if (!responseCode.equals(CliConstants.RESPONSE_OK)) {
                String resultString = CliUtils.getHttpResponseString(response);
                ExceptionMapper exception = gson.fromJson(resultString, ExceptionMapper.class);
                System.out.println(exception);
                return null;
            }

            Cluster cluster = getClusterObjectFromString(CliUtils.getHttpResponseString(response));

            if (cluster == null) {
                System.out.println("No existing subscriptions found for alias " + alias);
                return null;
            }

            Member[] members = new Member[cluster.getMember().size()];
            members = cluster.getMember().toArray(members);

            return members;
        } catch (Exception e) {
            String message = "Error in listing members";
            System.out.println(message);
            logger.error(message, e);
            return null;
        }
    }

    private Cluster getClusterObjectFromString(String resultString) {
        String tmp;
        if (resultString.startsWith("{\"cluster\"")) {
            tmp = resultString.substring("{\"cluster\"".length() + 1, resultString.length() - 1);
            resultString = tmp;
        }
        GsonBuilder gsonBuilder = new GsonBuilder();
        Gson gson = gsonBuilder.create();

        return gson.fromJson(resultString, Cluster.class);
    }

    private ArrayList<Cluster> getClusterListObjectFromString(String resultString) {
        GsonBuilder gsonBuilder = new GsonBuilder();
        Gson gson = gsonBuilder.create();

        ClusterList clusterlist = gson.fromJson(resultString, ClusterList.class);
        return clusterlist.getCluster();
    }

    private void printLBs(String resultString) {

        Cluster cluster = getClusterObjectFromString(resultString);

        if (cluster == null) {
            System.out.println("Subscribe cartridge list is null");
            return;
        }

        Member[] members = new Member[cluster.getMember().size()];
        members = cluster.getMember().toArray(members);

        if (members.length == 0) {
            if (logger.isDebugEnabled()) {
                logger.debug("No subscribed cartridges found");
            }
            System.out.println("There are no subscribed cartridges");
            return;
        }

        for (Member member : members) {
            System.out.println("\n\tServiceName : " + member.getServiceName());
            System.out.println("\tClusterId : " + member.getClusterId());
            System.out.println("\tNewtworkPartitionId : " + member.getNetworkPartitionId());
            System.out.println("\tPartitionId : " + member.getPartitionId());
            System.out.println("\tStatus : " + member.getStatus());
            if (member.getLbClusterId() != null) {
                System.out.println("\tLBCluster : " + member.getLbClusterId());
            }
            System.out.println("\tMemberPrivateIp : " + member.getMemberIp());
            System.out.println("\tMemberFloatingIp : " + member.getMemberPublicIp());
            System.out.println("\t-----------------------");
        }
    }

    private String getAsPolicyFromServiceDefinition(String cartridgeType) throws CommandException {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        try {
            HttpResponse response = restClient.doGet(httpClient, restClient.getBaseURL()
                    + LIST_SERVICE_ENDPOINT + "/" + cartridgeType);

            String responseCode = "" + response.getStatusLine().getStatusCode();

            GsonBuilder gsonBuilder = new GsonBuilder();
            Gson gson = gsonBuilder.create();

            if (!responseCode.equals(CliConstants.RESPONSE_OK)) {
                String resultString = CliUtils.getHttpResponseString(response);
                ExceptionMapper exception = gson.fromJson(resultString, ExceptionMapper.class);
                System.out.println(exception);
                return null;
            }

            String resultString = CliUtils.getHttpResponseString(response);
            if (resultString == null) {
                System.out.println("Response content is empty");
                return null;
            }

            String serviceDefinitionString = resultString.substring(25, resultString.length() - 1);
            ServiceDefinitionBean serviceDefinition = gson.fromJson(serviceDefinitionString, ServiceDefinitionBean.class);
            if (serviceDefinition == null) {
                System.out.println("Deploy service list is empty");
                return null;
            }

            return serviceDefinition.getAutoscalingPolicyName();

        } catch (Exception e) {
            handleException("Exception in listing deploy services", e);
            return null;
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }

    private String getDeploymentPolicyFromServiceDefinition(String cartridgeType) throws CommandException {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        try {
            HttpResponse response = restClient.doGet(httpClient, restClient.getBaseURL()
                    + LIST_SERVICE_ENDPOINT + "/" + cartridgeType);

            String responseCode = "" + response.getStatusLine().getStatusCode();

            GsonBuilder gsonBuilder = new GsonBuilder();
            Gson gson = gsonBuilder.create();

            if (!responseCode.equals(CliConstants.RESPONSE_OK)) {
                String resultString = CliUtils.getHttpResponseString(response);
                ExceptionMapper exception = gson.fromJson(resultString, ExceptionMapper.class);
                System.out.println(exception);
                return null;
            }

            String resultString = CliUtils.getHttpResponseString(response);
            if (resultString == null) {
                System.out.println("Response content is empty");
                return null;
            }

            String serviceDefinitionString = resultString.substring(25, resultString.length() - 1);
            ServiceDefinitionBean serviceDefinition = gson.fromJson(serviceDefinitionString, ServiceDefinitionBean.class);
            if (serviceDefinition == null) {
                System.out.println("Deploy service list is empty");
                return null;
            }

            return serviceDefinition.getDeploymentPolicyName();

        } catch (Exception e) {
            handleException("Exception in listing deploy services", e);
            return null;
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }

    // This method does the cartridge subscription
    public void subscribe(String cartridgeType, String alias, String externalRepoURL, boolean privateRepo, String username,
                          String password, String asPolicy,
                          String depPolicy, String size, boolean remoOnTermination, boolean persistanceMapping,
                          boolean enableCommits, String volumeId)
            throws CommandException {
        DefaultHttpClient httpClient = new DefaultHttpClient();

        CartridgeInfoBean cartridgeInfoBean = new CartridgeInfoBean();
        GsonBuilder gsonBuilder = new GsonBuilder();
        Gson gson = gsonBuilder.create();

        try {
            Cartridge cartridge = getCartridge(cartridgeType);
            if (cartridge == null) {
                System.out.println("Cartridge not found: " + cartridgeType);
                return;
            }

            if (cartridge.isMultiTenant()) {
                asPolicy = getAsPolicyFromServiceDefinition(cartridgeType);
                depPolicy = getDeploymentPolicyFromServiceDefinition(cartridgeType);
            }

            cartridgeInfoBean.setCartridgeType(cartridgeType);
            cartridgeInfoBean.setAlias(alias);
            cartridgeInfoBean.setRepoURL(externalRepoURL);
            cartridgeInfoBean.setPrivateRepo(privateRepo);
            cartridgeInfoBean.setRepoUsername(username);
            cartridgeInfoBean.setRepoPassword(password);
            cartridgeInfoBean.setAutoscalePolicy(asPolicy);
            cartridgeInfoBean.setDeploymentPolicy(depPolicy);
            cartridgeInfoBean.setSize(size);
            cartridgeInfoBean.setRemoveOnTermination(remoOnTermination);
            cartridgeInfoBean.setPersistanceRequired(persistanceMapping);
            cartridgeInfoBean.setCommitsEnabled(enableCommits);
            cartridgeInfoBean.setVolumeId(volumeId);

            String jsonSubscribeString = gson.toJson(cartridgeInfoBean, CartridgeInfoBean.class);

            HttpResponse response = restClient.doPost(httpClient, restClient.getBaseURL() + SUBSCRIBE_CARTRIDGE_ENDPOINT,
                    jsonSubscribeString);

            String responseCode = "" + response.getStatusLine().getStatusCode();

            if (!responseCode.equals(CliConstants.RESPONSE_OK)) {
                String resultString = CliUtils.getHttpResponseString(response);
                ExceptionMapper exception = gson.fromJson(resultString, ExceptionMapper.class);
                System.out.println(exception);
                return;
            }

            String subscriptionOutput = CliUtils.getHttpResponseString(response);

            if (subscriptionOutput == null) {
                System.out.println("Error in response");
                return;
            }

            String subscriptionOutputJSON = subscriptionOutput.substring(20, subscriptionOutput.length() - 1);
            SubscriptionInfo subcriptionInfo = gson.fromJson(subscriptionOutputJSON, SubscriptionInfo.class);

            System.out.format("You have successfully subscribed to %s cartridge with alias %s.%n", cartridgeType, alias);

            String repoURL;
            String hostnames = null;
            String hostnamesLabel = null;
            if (subcriptionInfo != null) {
                repoURL = subcriptionInfo.getRepositoryURL();
                hostnames = subcriptionInfo.getHostname();
                hostnamesLabel = "host name";

                if (repoURL != null) {
                    System.out.println("GIT Repository URL: " + repoURL);
                }
            }

            if (externalRepoURL != null) {
                String takeTimeMsg = "(this might take few minutes... depending on repo size)\n";
                System.out.println(takeTimeMsg);
            }

            System.out.format("Please map the %s \"%s\" to LB IP%n", hostnamesLabel, hostnames);
        } catch (Exception e) {
            handleException("Exception in subscribing to cartridge", e);
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }

    // This method does the cartridge subscription
    public void subscribe(String subscriptionJson)
            throws CommandException {

        DefaultHttpClient httpClient = new DefaultHttpClient();
        GsonBuilder gsonBuilder = new GsonBuilder();
        Gson gson = gsonBuilder.create();

        try {
            HttpResponse response = restClient.doPost(httpClient, restClient.getBaseURL() + SUBSCRIBE_CARTRIDGE_ENDPOINT,
                    subscriptionJson);

            String responseCode = "" + response.getStatusLine().getStatusCode();

            if (!responseCode.equals(CliConstants.RESPONSE_OK)) {
                String resultString = CliUtils.getHttpResponseString(response);
                ExceptionMapper exception = gson.fromJson(resultString, ExceptionMapper.class);
                System.out.println(exception);
                return;
            }

            String subscriptionOutput = CliUtils.getHttpResponseString(response);

            if (subscriptionOutput == null) {
                System.out.println("Error in response");
                return;
            }

            String subscriptionOutputJSON = subscriptionOutput.substring(20, subscriptionOutput.length() - 1);
            SubscriptionInfo subcriptionInfo = gson.fromJson(subscriptionOutputJSON, SubscriptionInfo.class);

            System.out.format("You have successfully subscribed. ");

            String repoURL;
            String hostnames = null;
            String hostnamesLabel = null;
            if (subcriptionInfo != null) {
                repoURL = subcriptionInfo.getRepositoryURL();
                hostnames = subcriptionInfo.getHostname();
                hostnamesLabel = "host name";

                if (repoURL != null) {
                    System.out.println("GIT Repository URL: " + repoURL);
                }
            }

            System.out.format("Please map the %s \"%s\" to LB IP%n", hostnamesLabel, hostnames);

        } catch (Exception e) {
            handleException("Exception in subscribing to cartridge", e);
        } finally {
            httpClient.getConnectionManager().shutdown();
        }

    }

    // This method helps to create the new tenant
    public void addTenant(String admin, String firstName, String lastaName, String password, String domain, String email)
            throws CommandException {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        try {
            TenantInfoBean tenantInfo = new TenantInfoBean();
            tenantInfo.setAdmin(admin);
            tenantInfo.setFirstname(firstName);
            tenantInfo.setLastname(lastaName);
            tenantInfo.setAdminPassword(password);
            tenantInfo.setTenantDomain(domain);
            tenantInfo.setEmail(email);

            GsonBuilder gsonBuilder = new GsonBuilder();
            Gson gson = gsonBuilder.create();

            String jsonString = gson.toJson(tenantInfo, TenantInfoBean.class);

            HttpResponse response = restClient.doPost(httpClient, restClient.getBaseURL()
                    + ADD_TENANT_ENDPOINT, jsonString);

            String responseCode = "" + response.getStatusLine().getStatusCode();

            if (responseCode.equals(CliConstants.RESPONSE_OK)) {
                System.out.println("Tenant added successfully");
                return;
            } else {
                String resultString = CliUtils.getHttpResponseString(response);
                ExceptionMapper exception = gson.fromJson(resultString, ExceptionMapper.class);
                System.out.println(exception);
            }

        } catch (Exception e) {
            handleException("Exception in creating tenant", e);
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }

    // This method helps to create the new user
    public void addUser(String userName, String credential, String role, String firstName, String lastName, String email, String profileName)
            throws CommandException {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        try {
            UserInfoBean userInfoBean = new UserInfoBean();
            userInfoBean.setUserName(userName);
            userInfoBean.setCredential(credential);
            userInfoBean.setRole(role);
            userInfoBean.setFirstName(firstName);
            userInfoBean.setLastName(lastName);
            userInfoBean.setEmail(email);
            userInfoBean.setProfileName(profileName);

            GsonBuilder gsonBuilder = new GsonBuilder();
            Gson gson = gsonBuilder.create();

            String jsonString = gson.toJson(userInfoBean, UserInfoBean.class);

            HttpResponse response = restClient.doPost(httpClient, restClient.getBaseURL()
                    + ADD_USER_ENDPOINT, jsonString);

            String responseCode = "" + response.getStatusLine().getStatusCode();

            if (responseCode.equals(CliConstants.RESPONSE_CREATED)) {
                System.out.println("User added successfully");
                return;
            } else {
                String resultString = CliUtils.getHttpResponseString(response);
                ExceptionMapper exception = gson.fromJson(resultString, ExceptionMapper.class);
                System.out.println(exception);
            }

        } catch (Exception e) {
            handleException("Exception in creating User", e);
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }

    // This method helps to delete the created tenant
    public void deleteTenant(String tenantDomain) throws CommandException {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        try {
            HttpResponse response = restClient.doDelete(httpClient, restClient.getBaseURL()
                    + ADD_TENANT_ENDPOINT + "/" + tenantDomain);

            String responseCode = "" + response.getStatusLine().getStatusCode();

            GsonBuilder gsonBuilder = new GsonBuilder();
            Gson gson = gsonBuilder.create();

            if (responseCode.equals(CliConstants.RESPONSE_OK)) {
                System.out.println("You have succesfully delete " + tenantDomain + " tenant");
                return;
            } else {
                String resultString = CliUtils.getHttpResponseString(response);
                ExceptionMapper exception = gson.fromJson(resultString, ExceptionMapper.class);
                System.out.println(exception);
            }

        } catch (Exception e) {
            handleException("Exception in deleting " + tenantDomain + " tenant", e);
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }

    // This method helps to delete the created user
    public void deleteUser(String userName) throws CommandException {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        try {
            HttpResponse response = restClient.doDelete(httpClient, restClient.getBaseURL()
                    + ADD_USER_ENDPOINT + "/" + userName);

            String responseCode = "" + response.getStatusLine().getStatusCode();

            GsonBuilder gsonBuilder = new GsonBuilder();
            Gson gson = gsonBuilder.create();

            if (responseCode.equals(CliConstants.RESPONSE_NO_CONTENT)) {
                System.out.println("You have succesfully deleted " + userName + " user");
                return;
            } else {
                String resultString = CliUtils.getHttpResponseString(response);
                ExceptionMapper exception = gson.fromJson(resultString, ExceptionMapper.class);
                System.out.println(exception);
            }

        } catch (Exception e) {
            handleException("Exception in deleting " + userName + " user", e);
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }

    // This method helps to deactivate the created tenant
    public void deactivateTenant(String tenantDomain) throws CommandException {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        try {
            HttpResponse response = restClient.doPost(httpClient, restClient.getBaseURL()
                    + DEACTIVATE_TENANT_ENDPOINT + "/" + tenantDomain, "");

            String responseCode = "" + response.getStatusLine().getStatusCode();

            GsonBuilder gsonBuilder = new GsonBuilder();
            Gson gson = gsonBuilder.create();

            if (responseCode.equals(CliConstants.RESPONSE_OK)) {
                System.out.println("You have succesfully deactivate " + tenantDomain + " tenant");
                return;
            } else {
                String resultString = CliUtils.getHttpResponseString(response);
                ExceptionMapper exception = gson.fromJson(resultString, ExceptionMapper.class);
                System.out.println(exception);
            }

        } catch (Exception e) {
            handleException("Exception in deactivating " + tenantDomain + " tenant", e);
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }

    // This method helps to activate, deactivated tenant
    public void activateTenant(String tenantDomain) throws CommandException {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        try {
            HttpResponse response = restClient.doPost(httpClient, restClient.getBaseURL()
                    + ACTIVATE_TENANT_ENDPOINT + "/" + tenantDomain, "");

            String responseCode = "" + response.getStatusLine().getStatusCode();

            GsonBuilder gsonBuilder = new GsonBuilder();
            Gson gson = gsonBuilder.create();

            if (responseCode.equals(CliConstants.RESPONSE_OK)) {
                System.out.println("You have succesfully activated tenant: " + tenantDomain);
                return;
            } else {
                String resultString = CliUtils.getHttpResponseString(response);
                ExceptionMapper exception = gson.fromJson(resultString, ExceptionMapper.class);
                System.out.println(exception);
            }

        } catch (Exception e) {
            handleException("Error in activating tenant: " + tenantDomain, e);
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }

    // This method helps to list all tenants
    public void listAllTenants() throws CommandException {
        try {
            TenantInfoList tenantInfoList = (TenantInfoList)restClient.listEntity(LIST_TENANT_ENDPOINT,
                    TenantInfoList.class, "tenants");

            if ((tenantInfoList == null) || (tenantInfoList.getTenantInfoBean() == null) ||
                    (tenantInfoList.getTenantInfoBean().size() == 0)) {
                System.out.println("No tenants found");
                return;
            }

            RowMapper<TenantInfoBean> rowMapper = new RowMapper<TenantInfoBean>() {
                public String[] getData(TenantInfoBean tenantInfo) {
                    String[] data = new String[5];
                    data[0] = tenantInfo.getTenantDomain();
                    data[1] = "" + tenantInfo.getTenantId();
                    data[2] = tenantInfo.getEmail();
                    data[3] = tenantInfo.isActive() ? "Active" : "De-active";
                    data[4] = tenantInfo.getCreatedDate();
                    return data;
                }
            };

            TenantInfoBean[] tenantArray = new TenantInfoBean[tenantInfoList.getTenantInfoBean().size()];
            tenantArray = tenantInfoList.getTenantInfoBean().toArray(tenantArray);

            System.out.println("Tenants:");
            CliUtils.printTable(tenantArray, rowMapper, "Domain", "Tenant ID", "Email", "State", "Created Date");
        } catch (Exception e) {
            String message = "Error in listing users";
            System.out.println(message);
            logger.error(message, e);
        }
    }

    // This method helps to list all users
    public void listAllUsers() throws CommandException {
        try {
            UserInfoList userInfoList = (UserInfoList) restClient.listEntity(LIST_USERS_ENDPOINT,
                    UserInfoList.class, "users");

            if ((userInfoList == null) || (userInfoList.getUserInfoBean() == null) ||
                    (userInfoList.getUserInfoBean().size() == 0)) {
                System.out.println("No users found");
                return;
            }

            RowMapper<UserInfoBean> rowMapper = new RowMapper<UserInfoBean>() {
                public String[] getData(UserInfoBean userInfo) {
                    String[] data = new String[2];
                    data[0] = userInfo.getUserName();
                    data[1] = userInfo.getRole();
                    return data;
                }
            };

            UserInfoBean[] usersArray = new UserInfoBean[userInfoList.getUserInfoBean().size()];
            usersArray = userInfoList.getUserInfoBean().toArray(usersArray);

            System.out.println("Users:");
            CliUtils.printTable(usersArray, rowMapper, "Username", "Role");
        } catch (Exception e) {
            String message = "Error in listing users";
            System.out.println(message);
            logger.error(message, e);
        }
    }

    // This method helps to unsubscribe cartridges
    public void unsubscribe(String alias) throws CommandException {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        try {
            HttpResponse response = restClient.doPost(httpClient, restClient.getBaseURL() + UNSUBSCRIBE_CARTRIDGE_OF_TENANT_ENDPOINT, alias);

            String responseCode = "" + response.getStatusLine().getStatusCode();

            GsonBuilder gsonBuilder = new GsonBuilder();
            Gson gson = gsonBuilder.create();

            if (responseCode.equals(CliConstants.RESPONSE_OK)) {
                System.out.println("You have successfully unsubscribed " + alias + " cartridge");
                return;
            } else {
                String resultString = CliUtils.getHttpResponseString(response);
                ExceptionMapper exception = gson.fromJson(resultString, ExceptionMapper.class);
                System.out.println(exception);
            }

        } catch (Exception e) {
            String message = "Error in un-subscribing cartridge";
            System.out.println(message);
            logger.error(message, e);
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }

    // This method helps to deploy cartridge definitions
    public void deployCartridgeDefinition(String cartridgeDefinition) throws CommandException {
        restClient.deployEntity(DEPLOY_CARTRIDGE_ENDPOINT, cartridgeDefinition, "cartridge");
    }

    // This method helps to undeploy cartridge definitions
    public void undeployCartrigdeDefinition(String id) throws CommandException {
        restClient.undeployEntity(DEPLOY_CARTRIDGE_ENDPOINT, "cartridge", id);
    }

    // This method helps to deploy partitions
    public void deployPartition(String partitionDefinition) throws CommandException {
        restClient.deployEntity(DEPLOY_PARTITION_ENDPOINT, partitionDefinition, "partition");
    }

    // This method helps to deploy autoscalling polices
    public void deployAutoscalingPolicy(String autoScalingPolicy) throws CommandException {
        restClient.deployEntity(DEPLOY_AUTOSCALING_POLICY_ENDPOINT, autoScalingPolicy, "autoscaling policy");
    }

    // This method helps to deploy multi-tenant service cluster
    public void deployService(String serviceDefinition) throws CommandException {
        restClient.deployEntity(DEPLOY_SERVICE_ENDPOINT, serviceDefinition, "service");
    }

    // This method helps to undeploy multi-tenant service cluster
    public void undeployService(String id) throws CommandException {
        restClient.undeployEntity(DEPLOY_SERVICE_ENDPOINT, "service", id);
    }

    public void listServices() throws CommandException {
        try {
            ServiceDefinitionList list = (ServiceDefinitionList) restClient.listEntity(LIST_SERVICE_ENDPOINT,
                    ServiceDefinitionList.class, "service");

            if ((list == null) || (list.getServiceDefinition() == null) || (list.getServiceDefinition().size() == 0)) {
                System.out.println("No services found");
                return;
            }

            RowMapper<ServiceDefinitionBean> rowMapper = new RowMapper<ServiceDefinitionBean>() {

                public String[] getData(ServiceDefinitionBean definition) {
                    String[] data = new String[6];
                    data[0] = definition.getCartridgeType();
                    data[1] = definition.getDeploymentPolicyName();
                    data[2] = definition.getAutoscalingPolicyName();
                    data[3] = definition.getClusterDomain();
                    data[4] = definition.getTenantRange();
                    data[5] = definition.getIsPublic() ? "Public" : "Private";
                    ;
                    return data;
                }
            };

            ServiceDefinitionBean[] array = new ServiceDefinitionBean[list.getServiceDefinition().size()];
            array = list.getServiceDefinition().toArray(array);

            System.out.println("Services found:");
            CliUtils.printTable(array, rowMapper, "Cartridge Type", "Deployment Policy Name",
                    "Autoscaling Policy Name", "Cluster Domain", "Tenant Range", "Accessibility");
        } catch (Exception e) {
            String message = "Error in listing services";
            System.out.println(message);
            logger.error(message, e);
        }
    }

    // This method helps to deploy deployment polices
    public void deployDeploymentPolicy(String deploymentPolicy) throws CommandException {
        restClient.deployEntity(DEPLOY_DEPLOYMENT_POLICY_ENDPOINT, deploymentPolicy, "deployment policy");
    }

    // This method list available partitons
    public void listPartitions() throws CommandException {
        try {
            PartitionList list = (PartitionList) restClient.listEntity(LIST_PARTITION_ENDPOINT,
                    PartitionList.class, "partitions");

            if ((list == null) || (list.getPartition() == null) || (list.getPartition().size() == 0)) {
                System.out.println("No partitions found");
                return;
            }

            RowMapper<Partition> rowMapper = new RowMapper<Partition>() {

                public String[] getData(Partition partition) {
                    String[] data = new String[3];
                    data[0] = partition.getId();
                    data[1] = partition.getProvider();
                    data[2] = partition.getIsPublic() ? "Public" : "Private";
                    return data;
                }
            };

            Partition[] partitions = new Partition[list.getPartition().size()];
            partitions = list.getPartition().toArray(partitions);

            System.out.println("Partitions found:");
            CliUtils.printTable(partitions, rowMapper, "ID", "Provider", "Accessibility");
        } catch (Exception e) {
            String message = "Error in listing partitions";
            System.out.println(message);
            logger.error(message, e);
        }
    }

    public void listAutoscalingPolicies() throws CommandException {
        try {
            AutoscalePolicyList list = (AutoscalePolicyList) restClient.listEntity(LIST_AUTOSCALING_POLICY_ENDPOINT,
                    AutoscalePolicyList.class, "autoscaling policies");

            if ((list == null) || (list.getAutoscalePolicy() == null) || (list.getAutoscalePolicy().size() == 0)) {
                System.out.println("No autoscaling policies found");
                return;
            }

            RowMapper<AutoscalePolicy> rowMapper = new RowMapper<AutoscalePolicy>() {

                public String[] getData(AutoscalePolicy policy) {
                    String[] data = new String[2];
                    data[0] = policy.getId();
                    data[1] = policy.getIsPublic() ? "Public" : "Private";
                    return data;
                }
            };

            AutoscalePolicy[] array = new AutoscalePolicy[list.getAutoscalePolicy().size()];
            array = list.getAutoscalePolicy().toArray(array);

            System.out.println("Autoscaling policies found:");
            CliUtils.printTable(array, rowMapper, "ID", "Accessibility");
        } catch (Exception e) {
            String message = "Error in listing autoscaling policies";
            System.out.println(message);
            logger.error(message, e);
        }
    }

    public void listDeploymentPolicies() throws CommandException {
        try {
            DeploymentPolicyList list = (DeploymentPolicyList) restClient.listEntity(LIST_DEPLOYMENT_POLICY_ENDPOINT,
                    DeploymentPolicyList.class, "deployment policies");

            if ((list == null) || (list.getDeploymentPolicy() == null) || (list.getDeploymentPolicy().size() == 0)) {
                System.out.println("No deployment policies found");
                return;
            }

            RowMapper<DeploymentPolicy> rowMapper = new RowMapper<DeploymentPolicy>() {

                public String[] getData(DeploymentPolicy policy) {
                    String[] data = new String[2];
                    data[0] = policy.getId();
                    data[1] = policy.getIsPublic() ? "Public" : "Private";
                    return data;
                }
            };

            DeploymentPolicy[] array = new DeploymentPolicy[list.getDeploymentPolicy().size()];
            array = list.getDeploymentPolicy().toArray(array);

            System.out.println("Deployment policies found:");
            CliUtils.printTable(array, rowMapper, "ID", "Accessibility");
        } catch (Exception e) {
            String message = "Error in listing deployment policies";
            System.out.println(message);
            logger.error(message, e);
        }
    }

    public void describeDeploymentPolicy(String id) throws CommandException {
        try {
            DeploymentPolicyList list = (DeploymentPolicyList) restClient.listEntity(LIST_DEPLOYMENT_POLICY_ENDPOINT,
                    DeploymentPolicyList.class, "deployment policies");

            if ((list == null) || (list.getDeploymentPolicy() == null) || (list.getDeploymentPolicy().size() == 0)) {
                System.out.println("Deployment policy not found: " + id);
                return;
            }

            for (DeploymentPolicy policy : list.getDeploymentPolicy()) {
                if (policy.getId().equals(id)) {
                    System.out.println("Deployment policy: " + id);
                    System.out.println(getGson().toJson(policy));
                    return;
                }
            }
            System.out.println("Deployment policy not found: " + id);
        } catch (Exception e) {
            String message = "Error in describing deployment policy: " + id;
            System.out.println(message);
            logger.error(message, e);
        }
    }

    public void describePartition(String id) throws CommandException {
        try {
            PartitionList list = (PartitionList) restClient.listEntity(LIST_PARTITION_ENDPOINT,
                    PartitionList.class, "partitions");

            if ((list == null) || (list.getPartition() == null) || (list.getPartition().size() == 0)) {
                System.out.println("Partition not found: " + id);
                return;
            }

            for (Partition partition : list.getPartition()) {
                if (partition.getId().equals(id)) {
                    System.out.println("Partition: " + id);
                    System.out.println(getGson().toJson(partition));
                    return;
                }
            }
            System.out.println("Partition not found: " + id);
        } catch (Exception e) {
            String message = "Error in describing partition: " + id;
            System.out.println(message);
            logger.error(message, e);
        }
    }

    public void describeAutoScalingPolicy(String id) throws CommandException {
        try {
            AutoscalePolicyList list = (AutoscalePolicyList) restClient.listEntity(LIST_AUTOSCALING_POLICY_ENDPOINT,
                    AutoscalePolicyList.class, "autoscaling policies");

            if ((list == null) || (list.getAutoscalePolicy() == null) || (list.getAutoscalePolicy().size() == 0)) {
                System.out.println("Autoscaling policy not found: " + id);
                return;
            }

            for (AutoscalePolicy policy : list.getAutoscalePolicy()) {
                if (policy.getId().equalsIgnoreCase(id)) {
                    System.out.println("Autoscaling policy: " + id);
                    System.out.println(getGson().toJson(policy));
                    return;
                }
            }
            System.out.println("Autoscaling policy not found: " + id);
        } catch (Exception e) {
            String message = "Error in describing autoscaling policy: " + id;
            System.out.println(message);
            logger.error(message, e);
        }
    }

    public void deployKubernetesGroup(String entityBody) {
        restClient.deployEntity(DEPLOY_KUBERNETES_GROUP_ENDPOINT, entityBody, "kubernetes group");
    }

    public void listKubernetesGroups() {
        try {
            KubernetesGroupList list = (KubernetesGroupList) restClient.listEntity(LIST_KUBERNETES_GROUP_ENDPOINT, KubernetesGroupList.class, "kubernetes group");
            if ((list != null) && (list.getKubernetesGroup() != null) && (list.getKubernetesGroup().size() > 0)) {
                RowMapper<KubernetesGroup> partitionMapper = new RowMapper<KubernetesGroup>() {
                    public String[] getData(KubernetesGroup kubernetesGroup) {
                        String[] data = new String[2];
                        data[0] = kubernetesGroup.getGroupId();
                        data[1] = kubernetesGroup.getDescription();
                        return data;
                    }
                };

                KubernetesGroup[] array = new KubernetesGroup[list.getKubernetesGroup().size()];
                array = list.getKubernetesGroup().toArray(array);
                System.out.println("Kubernetes groups found:");
                CliUtils.printTable(array, partitionMapper, "Group ID", "Description");
            } else {
                System.out.println("No kubernetes groups found");
                return;
            }
        } catch (Exception e) {
            String message = "Error in listing kubernetes groups";
            System.out.println(message);
            logger.error(message, e);
        }
    }

    public void undeployKubernetesGroup(String groupId) {
        restClient.undeployEntity(UNDEPLOY_KUBERNETES_GROUP_ENDPOINT, "kubernetes group", groupId);
    }

    public void deployKubernetesHost(String entityBody) {
        restClient.deployEntity(DEPLOY_KUBERNETES_HOST_ENDPOINT, entityBody, "kubernetes host");
    }

    public void listKubernetesHosts(String groupId) {
        try {
            KubernetesHostList list = (KubernetesHostList) restClient.listEntity(LIST_KUBERNETES_HOSTS_ENDPOINT.replace("{groupId}", groupId),
                    KubernetesHostList.class, "kubernetes host");
            if ((list != null) && (list.getKubernetesHost() != null) && (list.getKubernetesHost().size() > 0)) {
                RowMapper<KubernetesHost> partitionMapper = new RowMapper<KubernetesHost>() {
                    public String[] getData(KubernetesHost kubernetesHost) {
                        String[] data = new String[3];
                        data[0] = kubernetesHost.getHostId();
                        data[1] = kubernetesHost.getHostname();
                        data[2] = kubernetesHost.getHostIpAddress();
                        return data;
                    }
                };

                KubernetesHost[] array = new KubernetesHost[list.getKubernetesHost().size()];
                array = list.getKubernetesHost().toArray(array);
                System.out.println("Kubernetes hosts found:");
                CliUtils.printTable(array, partitionMapper, "Host ID", "Hostname", "IP Address");
            } else {
                System.out.println("No kubernetes hosts found");
                return;
            }
        } catch (Exception e) {
            String message = "Error in listing kubernetes hosts";
            System.out.println(message);
            logger.error(message, e);
        }
    }

    public void undeployKubernetesHost(String hostId) {
        restClient.undeployEntity(UNDEPLOY_KUBERNETES_HOST_ENDPOINT, "kubernetes host", hostId);
    }

    public void updateKubernetesMaster(String entityBody) {
        restClient.updateEntity(UPDATE_KUBERNETES_MASTER_ENDPOINT, entityBody, "kubernetes master");
    }

    public void updateKubernetesHost(String entityBody) {
        restClient.updateEntity(UPDATE_KUBERNETES_HOST_ENDPOINT, entityBody, "kubernetes host");
    }

    public void synchronizeArtifacts(String cartridgeAlias) throws CommandException {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        try {
            HttpResponse response = restClient.doPost(httpClient, restClient.getBaseURL() + SYNCHRONIZE_ARTIFACTS_ENDPOINT, cartridgeAlias);

            String responseCode = "" + response.getStatusLine().getStatusCode();

            if (responseCode.equals(CliConstants.RESPONSE_OK)) {
                System.out.println(String.format("Synchronizing artifacts for cartridge subscription alias: %s", cartridgeAlias));
                return;
            } else {
                GsonBuilder gsonBuilder = new GsonBuilder();
                Gson gson = gsonBuilder.create();
                String resultString = CliUtils.getHttpResponseString(response);
                ExceptionMapper exception = gson.fromJson(resultString, ExceptionMapper.class);
                System.out.println(exception);
            }
        } catch (Exception e) {
            String message = "Error in synchronizing artifacts for cartridge subscription alias: " + cartridgeAlias;
            System.out.println(message);
            logger.error(message, e);
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }

    // This class convert JSON string to deploymentpolicylist object
    private class DeploymentPolicyList {
        private ArrayList<DeploymentPolicy> deploymentPolicy;

        public ArrayList<DeploymentPolicy> getDeploymentPolicy() {
            return deploymentPolicy;
        }

        public void setDeploymentPolicy(ArrayList<DeploymentPolicy> deploymentPolicy) {
            this.deploymentPolicy = deploymentPolicy;
        }

        DeploymentPolicyList() {
            deploymentPolicy = new ArrayList<DeploymentPolicy>();
        }
    }

    // This class convert JSON string to autoscalepolicylist object
    private class AutoscalePolicyList {
        private ArrayList<AutoscalePolicy> autoscalePolicy;

        public ArrayList<AutoscalePolicy> getAutoscalePolicy() {
            return autoscalePolicy;
        }

        public void setAutoscalePolicy(ArrayList<AutoscalePolicy> autoscalePolicy) {
            this.autoscalePolicy = autoscalePolicy;
        }

        AutoscalePolicyList() {
            autoscalePolicy = new ArrayList<AutoscalePolicy>();
        }
    }

    // This class convert JSON string to servicedefinitionbean object
    private class ServiceDefinitionList {
        private ArrayList<ServiceDefinitionBean> serviceDefinitionBean;

        public ArrayList<ServiceDefinitionBean> getServiceDefinition() {
            return serviceDefinitionBean;
        }

        public void setServiceDefinition(ArrayList<ServiceDefinitionBean> serviceDefinitionBean) {
            this.serviceDefinitionBean = serviceDefinitionBean;
        }

        ServiceDefinitionList() {
            serviceDefinitionBean = new ArrayList<ServiceDefinitionBean>();
        }
    }

    // This class convert JSON string to PartitionLIst object
    private class PartitionList {
        private ArrayList<Partition> partition;

        public ArrayList<Partition> getPartition() {
            return partition;
        }

        public void setPartition(ArrayList<Partition> partition) {
            this.partition = partition;
        }

        PartitionList() {
            partition = new ArrayList<Partition>();
        }
    }

    // This class convert JSON string to TenantInfoBean object
    private class TenantInfoList {
        private ArrayList<TenantInfoBean> tenantInfoBean;

        public ArrayList<TenantInfoBean> getTenantInfoBean() {
            return tenantInfoBean;
        }

        public void setTenantInfoBean(ArrayList<TenantInfoBean> tenantInfoBean) {
            this.tenantInfoBean = tenantInfoBean;
        }

        TenantInfoList() {
            tenantInfoBean = new ArrayList<TenantInfoBean>();
        }
    }

    // This class convert JSON string to UserInfoBean object
    private class UserInfoList {
        private ArrayList<UserInfoBean> userInfoBean;

        public ArrayList<UserInfoBean> getUserInfoBean() {
            return userInfoBean;
        }

        public void setUserInfoBean(ArrayList<UserInfoBean> userInfoBean) {
            this.userInfoBean = userInfoBean;
        }

        UserInfoList() {
            userInfoBean = new ArrayList<UserInfoBean>();
        }
    }

    // This class is for convert JSON string to CartridgeList object
    private class CartridgeList {
        private ArrayList<Cartridge> cartridge;

        public ArrayList<Cartridge> getCartridge() {
            return cartridge;
        }

        public void setCartridge(ArrayList<Cartridge> cartridge) {
            this.cartridge = cartridge;
        }

        CartridgeList() {
            cartridge = new ArrayList<Cartridge>();
        }
    }

    private class ClusterList {
        private ArrayList<Cluster> cluster;

        public ArrayList<Cluster> getCluster() {
            return cluster;
        }

        public void setCluster(ArrayList<Cluster> clusters) {
            this.cluster = clusters;
        }

        ClusterList() {
            cluster = new ArrayList<Cluster>();
        }

        ;
    }

    // This will return access url from a given cartridge
    private String getAccessURLs(Cartridge cartridge) {
        PortMapping[] portMappings = cartridge.getPortMappings();
        StringBuilder urlBuilder = new StringBuilder();

        for (PortMapping portMapping : portMappings) {
            String url = portMapping.getProtocol() + "://" + cartridge.getHostName() + ":" + portMapping.getProxyPort() + "/";
            urlBuilder.append(url).append(", ");
        }

        return urlBuilder.toString();
    }

    // This is for handle exception
    private void handleException(String key, Exception e, Object... args) throws CommandException {
        if (logger.isDebugEnabled()) {
            logger.debug("Displaying message for {}. Exception thrown is {}", key, e.getClass());
        }

        String message = CliUtils.getMessage(key, args);

        if (logger.isErrorEnabled()) {
            logger.error(message);
        }

        System.out.println(message);
        throw new CommandException(message, e);
    }

    // This class is to convert JSON string to Cartridge object
    public class CartridgeWrapper {
        private Cartridge cartridge;

        public Cartridge getCartridge() {
            return cartridge;
        }

        public void setCartridge(Cartridge cartridge) {
            this.cartridge = cartridge;
        }

        public CartridgeWrapper() {
        }
    }

    public boolean isMultiTenant(String type) throws CommandException {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        try {
            HttpResponse response = restClient.doGet(httpClient, restClient.getBaseURL() + LIST_CARTRIDGES_ENDPOINT);

            String responseCode = "" + response.getStatusLine().getStatusCode();
            String resultString = CliUtils.getHttpResponseString(response);
            if (resultString == null) {
                return false;
            }

            GsonBuilder gsonBuilder = new GsonBuilder();
            Gson gson = gsonBuilder.create();

            if (!responseCode.equals(CliConstants.RESPONSE_OK)) {
                ExceptionMapper exception = gson.fromJson(resultString, ExceptionMapper.class);
                System.out.println(exception);
                return false;
            }

            CartridgeList cartridgeList = gson.fromJson(resultString, CartridgeList.class);

            if (cartridgeList == null) {
                System.out.println("Available cartridge list is null");
                return false;
            }

            ArrayList<Cartridge> multiTenetCartridge = new ArrayList<Cartridge>();

            for (Cartridge cartridge : cartridgeList.getCartridge()) {
                if (cartridge.isMultiTenant() && cartridge.getCartridgeType().equals(type)) {
                    multiTenetCartridge.add(cartridge);
                }
            }

            return multiTenetCartridge.size() > 0;

        } catch (Exception e) {
            handleException("Exception in listing cartridges", e);
            return false;
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }
}
