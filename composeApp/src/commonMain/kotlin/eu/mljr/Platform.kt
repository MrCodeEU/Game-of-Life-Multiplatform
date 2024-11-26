package eu.mljr

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform