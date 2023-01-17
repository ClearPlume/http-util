package top.fallenangel.tools.http

import com.fasterxml.jackson.annotation.JsonProperty

data class Result(
    val code: String,
    val data: LoginResult,
    @field:JsonProperty("code_detail")
    val codeDetail: String
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
    @field:JsonProperty("role_id")
    val roleId: Int,
    @field:JsonProperty("project_id")
    val projectId: Int,
)
