    /*********************************************************
     *    This file is licensed under the MIT 2.0 license    *
     *              Last updated June 6th, 2018              *
     *   *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *   *
     *    Please check out the full repository located at    *
     *   http://github.com/almic/CSGO-Skill for some other   *
     * important things like the User Agreement and Privacy  *
     *  Policy, as well as some helpful information on how   *
     *     you can contribute directly to the project :)     *
     *********************************************************/

// This file holds an object map for various errors.

/**
 * @apiDefine E_AlreadyExists
 * @apiVersion 0.1.0
 * @apiError (Error) AlreadyExists Specifies that, during signup, the account is already registered
 */
const ALREADY_EXISTS = 'AlreadyExists'

/**
 * @apiDefine E_AuthCanceled
 * @apiVersion 0.1.0
 * @apiError (Error) AuthCanceled OpenID specific error, authentication was canceled by the user
 */
const AUTH_CANCELED = 'AuthCanceled'

/**
 * @apiDefine E_BadEmail
 * @apiVersion 0.1.0
 * @apiError (Error) BadEmail Invalid email address
 */
const BAD_EMAIL = 'BadEmail'

/**
 * @apiDefine E_BadId
 * @apiVersion 0.1.0
 * @apiError (Error) BadId Invalid Steam ID
 */
const BAD_ID = 'BadId'

/**
 * @apiDefine E_BadMongos
 * @apiVersion 0.1.0
 * @apiError (Error) BadMongos Mongo DB internal problems
 */
const BAD_MONGOS = 'BadMongos'

/**
 * @apiDefine E_BadPersona
 * @apiVersion 0.1.0
 * @apiError (Error) BadPersona Invalid fursona name
 */
const BAD_PERSONA = 'BadPersona'

/**
 * @apiDefine E_BadRequest
 * @apiVersion 0.1.0
 * @apiError (Error) BadRequest Incorrect request, missing fields, bad data
 */
const BAD_REQUEST = 'BadRequest'

/**
 * @apiDefine E_BadToken
 * @apiVersion 0.1.0
 * @apiError (Error) BadToken Invalid Token object ({time: 123, value: "abcdef0123456789"})
 */
const BAD_TOKEN = 'BadToken'

/**
 * @apiDefine E_BadUsername
 * @apiVersion 0.1.0
 * @apiError (Error) BadUsername Invalid username
 */
const BAD_USERNAME = 'BadUsername'

/**
 * @apiDefine E_EmailTaken
 * @apiVersion 0.1.0
 * @apiError (Error) EmailTaken The provided email is already taken
 */
const EMAIL_TAKEN = 'EmailTaken'

/**
 * @apiDefine E_LoginRequired
 * @apiVersion 0.1.0
 * @apiError (Error) LoginRequired Indicates that the operation requires a fresh login, such as the token expiring
 */
const LOGIN_REQUIRED = 'LoginRequired'

/**
 * @apiDefine E_MultipleEmailChanges
 * @apiVersion 0.1.0
 * @apiError (Error) MultipleEmailChanges When a user attempts to change their email to frequently, or before a previous email is verified
 */
const MULTIPLE_EMAIL_CHANGES = 'MultipleEmailChanges'

/**
 * @apiDefine E_NameEmailTaken
 * @apiVersion 0.1.0
 * @apiError (Error) NameEmailTaken A special case where the username and email are taken, we tell the user they may already have an account, so that they may use that one instad of attempting to register another
 */

/**
 * @apiDefine E_NameTaken
 * @apiVersion 0.1.0
 * @apiError (Error) NameTaken The provided username is already taken
 */
const NAME_TAKEN = 'NameTaken'

/**
 * @apiDefine E_NoMongos
 * @apiVersion 0.1.0
 * @apiError (Error) NoMongos Unable to connect to Mongo DB
 */
const NO_MONGOS = 'NoMongos'

/**
 * @apiDefine E_NoGame
 * @apiVersion 0.1.0
 * @apiError (Error) NoGame The Steam account does not own CS: GO
 */
const NO_GAME = 'NoGame'
//    NO LIFE

/**
 * @apiDefine E_NotFound
 * @apiVersion 0.1.0
 * @apiError (Error) NotFound Generic error for a profile not found
 *
 * This can also be returned when a profile exists, but the authentication was incorrect, as to
 * prevent exposing the profile.
 */
const NOT_FOUND = 'NotFound'

/**
 * @apiDefine E_OpenIdError
 * @apiVersion 0.1.0
 * @apiError (Error) OpenIdErrorX Generic OpenID error, where X is a unique error number
 */
const OPENID_ERROR = 'OpenIdError'

/**
 * @apiDefine E_PlaytimeRequired
 * @apiVersion 0.1.0
 * @apiError (Error) PlaytimeRequired CS: GO playtime does not meet the minimum
 */
const PLAYTIME = 'PlaytimeRequired'

/**
 * @apiDefine E_PlaytimePrivate
 * @apiVersion 0.1.0
 * @apiError (Error) PlaytimePrivate CS: GO playtime is likely private
 */
const PLAYTIME_PRIVATE = 'PlaytimePrivate'

/**
 * @apiDefine E_PrivateAccount
 * @apiVersion 0.1.0
 * @apiError (Error) PrivateAccount The Steam account is totally private
 */
const PRIVATE_ACCOUNT = 'PrivateAccount'

/**
 * @apiDefine E_PrivateStatsOrDown
 * @apiVersion 0.1.0
 * @apiError (Error) PrivateStatsOrDown When a Steam stats request gives a 500 error
 *
 * This happens in both cases where the stats are private or if Steam is actually returning a legit
 * 500 Internal Server Error. Fucking seriously, Valve?
 */
const PRIVATE_STATS_OR_DOWN = 'PrivateStatsOrDown'

/**
 * @apiDefine E_ValidateFailed
 * @apiVersion 0.1.0
 * @apiError (Error) ValidateFailed Generally speaking, something didn't add up correctly with the Steam ID
 *
 * Given when a Steam API request didn't return the exact same Steam ID as requested,
 * probably when two or more Steam IDs were sent, or if the SteamID was some special
 * object or injected data.
 */
const VALIDATE_FAILED = 'ValidateFailed'

module.exports = {
    ALREADY_EXISTS,
    AUTH_CANCELED,

    BAD_EMAIL,
    BAD_ID,
    BAD_MONGOS,
    BAD_PERSONA,
    BAD_REQUEST,
    BAD_TOKEN,
    BAD_USERNAME,

    EMAIL_TAKEN,

    LOGIN_REQUIRED,

    MULTIPLE_EMAIL_CHANGES,

    NAME_TAKEN,
    NO_MONGOS,
    NO_GAME,
    NOT_FOUND,

    OPENID_ERROR,

    PLAYTIME,
    PLAYTIME_PRIVATE,
    PRIVATE_ACCOUNT,
    PRIVATE_STATS_OR_DOWN,

    VALIDATE_FAILED,
}
