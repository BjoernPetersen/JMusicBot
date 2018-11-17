package com.github.bjoernpetersen.musicbot.spi.auth

import com.github.bjoernpetersen.musicbot.api.auth.FullUser
import com.github.bjoernpetersen.musicbot.api.auth.Permission

interface UserDatabase {
    fun findUser(name: String): FullUser?

    fun getUsers(): Set<FullUser>

    @Throws(DuplicateUserException::class)
    fun insertUser(user: FullUser)

    fun updatePassword(user: FullUser)

    fun updatePermissions(name: String, permissions: Set<Permission>)

    fun deleteUser(name: String)

    fun close()
}
