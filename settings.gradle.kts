rootProject.name = "janus"

include(
    //Platform
    "platform:policy-store",
    "platform:state-store",
    "platform:decider",

    //Client
    "client:sidecar",

    //API
    "platform:api:policy-store-api",
    "platform:api:state-store-api",
    "client:api:sidecar-api",

    //SDK
    "sdk:java:annotations",
    "sdk:java:core",
    "sdk:java:spring-boot-starter",

    //Admin UI
    "admin-ui"
)
