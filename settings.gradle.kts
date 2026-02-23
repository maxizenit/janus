rootProject.name = "janus"

include(
    //Platform
    "platform:policy-store",
    "platform:state-store",

    //API
    "platform:api:policy-store-api",
    "platform:api:state-store-api",
    "client:api:sidecar",

    //SDK
    "sdk:java:annotations"
)
