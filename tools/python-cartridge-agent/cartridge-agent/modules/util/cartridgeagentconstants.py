JNDI_PROPERTIES_DIR = "jndi.properties.dir"
PARAM_FILE_PATH = "param.file.path"
EXTENSIONS_DIR = "extensions.dir"

MB_IP = "mb.ip"
MB_PORT = "mb.port"

INSTANCE_STARTED_SH = "instance-started.sh"
START_SERVERS_SH = "start-servers.sh"
INSTANCE_ACTIVATED_SH = "instance-activated.sh"
ARTIFACTS_UPDATED_SH = "artifacts-updated.sh"
CLEAN_UP_SH = "clean.sh"
MOUNT_VOLUMES_SH = "mount_volumes.sh"
SUBSCRIPTION_DOMAIN_ADDED_SH = "subscription-domain-added.sh"
SUBSCRIPTION_DOMAIN_REMOVED_SH = "subscription-domain-removed.sh"

CARTRIDGE_KEY = "CARTRIDGE_KEY"
APP_PATH = "APP_PATH"
SERVICE_GROUP = "SERIVCE_GROUP"
SERVICE_NAME = "SERVICE_NAME"
CLUSTER_ID = "CLUSTER_ID"
LB_CLUSTER_ID = "LB_CLUSTER_ID"
NETWORK_PARTITION_ID = "NETWORK_PARTITION_ID"
PARTITION_ID = "PARTITION_ID"
MEMBER_ID = "MEMBER_ID"
TENANT_ID= "TENANT_ID"
REPO_URL = "REPO_URL"
PORTS = "PORTS"
DEPLOYMENT = "DEPLOYMENT"
MANAGER_SERVICE_TYPE = "MANAGER_SERVICE_TYPE"
WORKER_SERVICE_TYPE = "WORKER_SERVICE_TYPE"

# stratos.sh environment variables keys
LOG_FILE_PATHS = "LOG_FILE_PATHS"
MEMORY_CONSUMPTION = "memory_consumption"
LOAD_AVERAGE = "load_average"
PORTS_NOT_OPEN = "ports_not_open"
MULTITENANT = "MULTITENANT"
CLUSTERING = "CLUSTERING"
MIN_INSTANCE_COUNT = "MIN_COUNT"
ENABLE_ARTIFACT_UPDATE = "enable.artifact.update"
ARTIFACT_UPDATE_INTERVAL = "artifact.update.interval"
COMMIT_ENABLED = "COMMIT_ENABLED"
AUTO_COMMIT = "auto.commit"
AUTO_CHECKOUT = "auto.checkout"
LISTEN_ADDRESS = "listen.address"
PROVIDER = "PROVIDER"
INTERNAL = "internal"
LB_PRIVATE_IP = "lb.private.ip"
LB_PUBLIC_IP = "lb.public.ip"

# stratos.sh extension points shell scripts names keys
INSTANCE_STARTED_SCRIPT = "extension.instance.started"
START_SERVERS_SCRIPT = "extension.start.servers"
INSTANCE_ACTIVATED_SCRIPT = "extension.instance.activated"
ARTIFACTS_UPDATED_SCRIPT = "extension.artifacts.updated"
CLEAN_UP_SCRIPT = "extension.clean"
MOUNT_VOLUMES_SCRIPT = "extension.mount.volumes"
MEMBER_ACTIVATED_SCRIPT = "extension.member.activated"
MEMBER_TERMINATED_SCRIPT = "extension.member.terminated"
MEMBER_SUSPENDED_SCRIPT = "extension.member.suspended"
MEMBER_STARTED_SCRIPT = "extension.member.started"
COMPLETE_TOPOLOGY_SCRIPT = "extension.complete.topology"
COMPLETE_TENANT_SCRIPT = "extension.complete.tenant"
SUBSCRIPTION_DOMAIN_ADDED_SCRIPT = "extension.subscription.domain.added"
SUBSCRIPTION_DOMAIN_REMOVED_SCRIPT = "extension.subscription.domain.removed"
ARTIFACTS_COPY_SCRIPT = "extension.artifacts.copy"
TENANT_SUBSCRIBED_SCRIPT = "extension.tenant.subscribed"
TENANT_UNSUBSCRIBED_SCRIPT = "extension.tenant.unsubscribed"

SERVICE_GROUP_TOPOLOGY_KEY = "payload_parameter.SERIVCE_GROUP"
CLUSTERING_TOPOLOGY_KEY = "payload_parameter.CLUSTERING"
CLUSTERING_PRIMARY_KEY = "PRIMARY"

SUPERTENANT_TEMP_PATH = "/tmp/-1234/"

DEPLOYMENT_MANAGER = "manager"
DEPLOYMENT_WORKER = "worker"
DEPLOYMENT_DEFAULT = "default"
SUPER_TENANT_REPO_PATH = "super.tenant.repository.path"
TENANT_REPO_PATH = "tenant.repository.path"

# topic names to subscribe
INSTANCE_NOTIFIER_TOPIC = "instance/#"
HEALTH_STAT_TOPIC = "health/#"
TOPOLOGY_TOPIC = "topology/#"
TENANT_TOPIC = "tenant/#"
INSTANCE_STATUS_TOPIC = "instance/#"


#Messaging Model
TENANT_RANGE_DELIMITER = "-"