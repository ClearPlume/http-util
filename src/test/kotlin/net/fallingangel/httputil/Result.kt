package net.fallingangel.httputil

import com.fasterxml.jackson.annotation.JsonProperty

data class Result(
    val code: String,
    @field:JsonProperty("code_detail")
    val codeDetail: String,
    val data: LoginResult,
    val msg: String?
)

data class LoginResult(
    val token: String,
    @field:JsonProperty("user_info")
    val userInfo: UserResult
)

data class UserResult(
    @field:JsonProperty("user_id")
    val userId: Int,
    val name: String,
    val roles: List<String>,
    val auths: List<String>,
    @field:JsonProperty("project_id")
    val projectId: Int,
)
