package ru.ridecorder.data.local.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "user_prefs")

class UserDataStore(private val context: Context) {
    companion object {
        private val USERID_KEY = stringPreferencesKey("user_id")
        private val USERNAME_KEY = stringPreferencesKey("user_name")
        private val FULLNAME_KEY = stringPreferencesKey("user_full_name")
        private val AVATAR_KEY = stringPreferencesKey("user_avatar_url")
        private val WEIGHT_KEY = stringPreferencesKey("user_weight")
        private val GENDER_KEY = stringPreferencesKey("user_gender")
    }


    val userFlow: Flow<User?> = context.dataStore.data.map { preferences ->
        val userId = preferences[USERID_KEY]
        val username = preferences[USERNAME_KEY]
        val fullName = preferences[FULLNAME_KEY]
        val avatarUrl = preferences[AVATAR_KEY]
        val weight = preferences[WEIGHT_KEY]?.toFloatOrNull()
        val gender = preferences[GENDER_KEY]
        if (userId != null) {
            User(userId.toInt(), username, fullName, avatarUrl, weight, gender)
        } else {
            null
        }
    }

    suspend fun saveUser(user: User) {
        context.dataStore.edit { preferences ->
            preferences[USERID_KEY] = user.userId.toString()
            user.userName?.let { preferences[USERNAME_KEY] = it }
            user.userFullName?.let { preferences[FULLNAME_KEY] = it }
            user.userAvatarUrl?.let { preferences[AVATAR_KEY] = it }
            user.weight?.let { preferences[WEIGHT_KEY] = it.toString() }
            user.gender?.let { preferences[GENDER_KEY] = it }
        }
    }

    suspend fun clearUser() {
        context.dataStore.edit { it.clear() }
    }
}

data class User(
    val userId: Int,
    val userName: String?,
    val userFullName: String?,
    val userAvatarUrl: String? = null,
    val weight: Float? = null,
    val gender: String? = null
)