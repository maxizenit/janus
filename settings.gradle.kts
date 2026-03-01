rootProject.name = "janus"

include(
    //Platform
    "platform:policy-store",
    "platform:state-store",
    "platform:decider",

    //API
    "platform:api:policy-store-api",
    "platform:api:state-store-api",
    "client:api:sidecar-api",

    //SDK
    "sdk:java:annotations"
)
