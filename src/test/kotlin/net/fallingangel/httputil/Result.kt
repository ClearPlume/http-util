package net.fallingangel.httputil

data class Result(
    val info: Info,
    val results: List<User>
)

data class User(
    val name: Name,
    val email: String,
    val phone: String,
    val gender: Gender,
    val location: Location
)

data class Name(
    val first: String,
    val title: String,
    val last: String
)

@Suppress("EnumEntryName")
enum class Gender {
    male, female
}

data class Location(
    val country: String,
    val state: String,
    val city: String,
    val street: Street,
    val postcode: Any,
    val timezone: Timezone,
    val coordinates: Coordinates
)

data class Street(
    val name: String,
    val number: Int
)

data class Timezone(
    val offset: String,
    val description: String
)

data class Coordinates(
    val latitude: String,
    val longitude: String
)

data class Info(
    val version: String,
    val results: Int,
    val page: Int,
    val seed: String
)
