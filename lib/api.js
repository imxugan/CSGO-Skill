    /*********************************************************
     *    This file is licensed under the MIT 2.0 license    *
     *              Last updated June 29th, 2018             *
     *   *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *   *
     *    Please check out the full repository located at    *
     *   http://github.com/almic/CSGO-Skill for some other   *
     * important things like the User Agreement and Privacy  *
     *  Policy, as well as some helpful information on how   *
     *     you can contribute directly to the project :)     *
     *********************************************************/

// This file is meant to contain all API handling, separate from server.js
const express = require('express')
    , ejs = require('ejs')
    , mailer = require('@sendgrid/mail')

const assistant = require('./assistant')
    , skill = require('./mongo.js')
    , LightSteamID = require('./openid.js')

const {
    BAD_ID,
    BAD_REQUEST,
    BAD_TOKEN,
    LOGIN_REQUIRED,
    NOT_FOUND,
    AUTH_CANCELED,
    BAD_EMAIL,
    BAD_USERNAME,
    BAD_PERSONA,
    ALREADY_EXISTS,
    NAME_TAKEN,
    EMAIL_TAKEN,
    MULTIPLE_EMAIL_CHANGES,
    BAD_MONGOS,
    OPENID_ERROR,
} = require('./errors.js')

// const MONGOURL = assistant.MONGOURL
// const STEAMKEY = assistant.STEAMKEY
const template = assistant.template

mailer.setApiKey(assistant.MAILKEY)

module.exports = api()

function api() {

    var router = express()

    router.on('mount', () => {
        // Ensure mounter has already called skill.connect()
        if (skill.exists()) console.log('API Router mounted')
        else throw new Error('Failed to mount API Router')
    })

    router.use((req, res, next) => {
        // Bailout quickly if not in the api subdomain
        if (req.subdomains[0] !== 'api') return next('router')
        next()
    })

    // From here on, we are in the api subdomain, so there is no need to use next()

    // For api documentation
    router.use('/docs', express.static('apidoc/doc'))

    /**
     * @api {get} /version API Version
     * @apiVersion 0.1.0
     * @apiName GetVersion
     * @apiGroup Static
     * @apiDescription Returns the current project version, syncronized between the website and apps.
     *                 This is represented by a list of MAJOR, MINOR, and PATCH versions.
     *
     * @apiSuccessExample {json} Success:
     *     [0, 1, 0]
     */
    router.all('/version', function (req, res) {
        // Current version is v0.1.0
        return res.json([0,1,0])
    })

/*
 *    ########   #######   ######  ########    ##        #######   ######   #### ##    ##
 *    ##     ## ##     ## ##    ##    ##       ##       ##     ## ##    ##   ##  ###   ##
 *    ##     ## ##     ## ##          ##       ##       ##     ## ##         ##  ####  ##
 *    ########  ##     ##  ######     ##       ##       ##     ## ##   ####  ##  ## ## ##
 *    ##        ##     ##       ##    ##       ##       ##     ## ##    ##   ##  ##  ####
 *    ##        ##     ## ##    ##    ##       ##       ##     ## ##    ##   ##  ##   ###
 *    ##         #######   ######     ##       ########  #######   ######   #### ##    ##
 */

    /**
     * @api {post} /login POST
     * @apiVersion     0.1.0
     * @apiName        PostLogin
     * @apiGroup       Login
     * @apiDescription Let's the user refresh their login token by posting the current one, probably
     *                 not optimal security but honestly this service isn't that crazy right now.
     *
     * @apiParam {String} steamId User's Steam ID
     * @apiParam {String} token   User's token hash
     *
     * @apiUse E_BadId
     * @apiUse E_BadMongos
     * @apiUse E_BadRequest
     * @apiUse E_BadToken
     * @apiUse E_LoginRequired
     * @apiUse E_NotFound
     *
     * @apiSuccess {Object} profile Full profile of the user
     *
     * @apiSuccessExample {json} Success:
     * {
     *   "success": true,
     *   "profile": {..}
     * }
     *
     * @apiErrorExample {json} Error:
     * {
     *   "success": false,
     *   "reason": "LoginRequired"
     * }
     */
    router.post('/login', (req, res) => {
        var post = req.body
        if (typeof post.steamId !== 'string' || !post.steamId ||
            typeof post.token   !== 'string' || !post.token)
            return res.json({
                'success': false,
                'reason': BAD_REQUEST
            })

        if (!assistant.isSteamId(post.steamId))
            return res.json({
                'success': false,
                'reason': BAD_ID
            })

        if (!assistant.isValidToken({value:post.token,time:Number(new Date())}))
            return res.json({
                'success': false,
                'reason': BAD_TOKEN
            })

        skill.db('collection:Players', (error, db, col) => {
            if (assistant.e(200, error)) {
                db.close()
                return res.json({
                    'success': false,
                    'reason': BAD_MONGOS
                })
            }
            col.findOne({steam_id:post.steamId}, (error, player) => {
                if (assistant.e(224, error)) {
                    db.close()
                    return res.json({
                        'success': false,
                        'reason': BAD_MONGOS
                    })
                }
                if (player && player.steam_id === post.steamId) {
                    if (assistant.isValidToken(player.token) &&
                        player.token.value === post.token) {

                        // Create new token
                        var token = assistant.getToken()

                        // Validate account and update sumaries
                        var valid = assistant.validateAccount(player.steam_id)
                        if (!valid.success) {
                            // Add to notification
                            player.notify = valid.reason
                        } else {
                            player.persona = valid.persona
                            player.profileurl = valid.profileurl
                            player.avatarurl = valid.avatarurl
                        }

                        col.findOneAndUpdate({'steam_id': player.steam_id},
                            {$set: {
                                'token': token,
                                'persona': player.persona,
                                'profileurl': player.profileurl,
                                'avatarurl': player.avatarurl
                            }}, (err, _) => {

                            db.close()
                            if (assistant.e(225, err))
                                return res.json({
                                    'success': false,
                                    'reason': BAD_MONGOS
                                })

                            // Update active
                            skill.db('collection:Stats', (error, db, col) => {
                                if (assistant.e(227, error)) {
                                    db.close()
                                    return
                                }
                                col.findOneAndUpdate({'steam_id': player.steam_id},
                                    {$set: {'active': Number(new Date())}}, (err, _) => {
                                    db.close()
                                    assistant.e(226, err)
                                })
                            })

                            // Notify player to complete verification
                            if (typeof player.verified === 'boolean' && !player.verified)
                                player.notify = 'email-verify'

                            player.token = token
                            delete player._id
                            return res.json({
                                success: true,
                                profile: player
                            })
                        })
                    } else {
                        db.close()
                        return res.json({
                            'success': false,
                            'reason': LOGIN_REQUIRED
                        })
                    }
                } else {
                    db.close()
                    return res.json({
                        'success': false,
                        'reason': NOT_FOUND
                    })
                }
            })
        })
    })


/*
 *     ######   ######## ########    ##        #######   ######   #### ##    ##
 *    ##    ##  ##          ##       ##       ##     ## ##    ##   ##  ###   ##
 *    ##        ##          ##       ##       ##     ## ##         ##  ####  ##
 *    ##   #### ######      ##       ##       ##     ## ##   ####  ##  ## ## ##
 *    ##    ##  ##          ##       ##       ##     ## ##    ##   ##  ##  ####
 *    ##    ##  ##          ##       ##       ##     ## ##    ##   ##  ##   ###
 *     ######   ########    ##       ########  #######   ######   #### ##    ##
 */

    /**
     * @api {get} /login GET
     * @apiVersion     0.1.0
     * @apiName        GetLogin
     * @apiGroup       Login
     * @apiDescription Used to initially log users in. Simply redirects new requests to the Steam
     *                 Open ID login page, and takes Open ID requests to finish the login process.
     *                 NOTE: Responses are sent through the javascript console messages, it can also
     *                 be found in a hidden span element named "out".
     *
     * @apiUse E_AuthCanceled
     * @apiUse E_BadMongos
     * @apiUse E_OpenIdError
     *
     * @apiSuccess {Object} profile Full profile of the user
     *
     * @apiSuccessExample {json} Success:
     * {
     *   "success": true,
     *   "profile": {..}
     * }
     *
     * @apiErrorExample {json} Error:
     * {
     *   "success": false,
     *   "reason": "OpenIdError3"
     * }
     */
    router.get('/login', (req, res) => {
        const openid = new LightSteamID('http://api.csgo-skill.com/login', req)
        if (!openid.mode) {
            res.status(302).location(openid.authUrl)
            return res.send()
        }
        if (openid.mode === 'cancel')
            return res.send(assistant.consoleResponse({
                'success': false,
                'reason': AUTH_CANCELED
            }))

        if (openid.validate()) {
            var steamId = openid.steamId
            try {
                skill.db('collection:Players', (err, db, col) => {
                    if (assistant.e(201, err)) {
                        db.close()
                        return res.send(assistant.consoleResponse({
                            'success': false,
                            'reason': BAD_MONGOS
                        }))
                    }
                    col.find({'steam_id': steamId}).toArray((err, docs) => {
                        if (assistant.e(202, err)) {
                            db.close()
                            return res.send(assistant.consoleResponse({
                                'success': false,
                                'reason': BAD_MONGOS
                            }))
                        }

                        if (docs.length === 1) {
                            var player = docs[0]
                            delete player._id

                            // Notify player to complete verification
                            if (typeof player.verified === 'boolean' && !player.verified)
                                player.notify = 'email-verify'

                            /*
                             We do NOT update the player summaries for GET logins, as these are
                             immediately followed by a POST login request in the Main.kt android
                             class. This is because GET logins are infrequent, as often POST is
                             used first, which means that even if a GET login occures, Main.kt still
                             does a POST login right after. This simplifies the android code by
                             avoiding the hassle of checking if the player recieved was already
                             logged in via GET.
                             */

                            // Update the token and return
                            var token = assistant.getToken()
                            player.token = token
                            col.findOneAndUpdate({'steam_id': steamId},
                                {$set: { 'token': token }}, (err, _) => {
                                // Not a big deal, but log anyway
                                db.close()
                                assistant.e(203, err)
                            })
                            return res.send(assistant.consoleResponse({
                                success: true,
                                profile: player
                            }))
                        }

                        if (docs.length !== 0) {
                            // Strange. Just error out
                            db.close()
                            return res.send(assistant.consoleResponse({
                                'success': false,
                                'reason': BAD_MONGOS
                            }))
                        }

                        // New account, check and add it!
                        var valid = assistant.validateAccount(steamId)
                        if (!valid.success) {
                            db.close()
                            return res.send(assistant.consoleResponse(valid))
                        }
                        col.insertOne(valid.profile, {}, (err, _) => {
                            db.close()
                            if (assistant.e(205, err))
                                return res.send(assistant.consoleResponse({
                                    'success': false,
                                    'reason': BAD_MONGOS
                                }))

                            skill.db('collection:Stats', (err, db, col) => {
                                if (assistant.e(206, err)) return
                                col.insertOne(assistant.getStatTemplate(steamId), {}, (err, _) => {
                                    db.close()
                                    assistant.e(207, err)
                                })
                            })
                            return res.send(assistant.consoleResponse(valid))
                        })
                    })
                })
            } catch (e) {
                try {
                    return res.send(assistant.consoleResponse({
                        'success': false,
                        'reason': BAD_MONGOS
                    }))
                } catch (e) {
                    // Error resulted from response, just log it
                    return assistant.e(209, e)
                }
            }
        } else if (openid.errno !== 0) {
            assistant.errorOut(openid.errno, openid.error)
            return res.send(assistant.consoleResponse({
                'success': false,
                'reason': OPENID_ERROR + openid.errno
            }))
        } else {
            // Unable to validate. Perhaps reused or fabricated request.
            return res.send(assistant.consoleResponse({
                'success': false,
                'reason': `We don't want your chocolate, ${openid.steamId}!`
            }))
        }
    })


/*
 *     ######  ####  ######   ##    ## ##     ## ########
 *    ##    ##  ##  ##    ##  ###   ## ##     ## ##     ##
 *    ##        ##  ##        ####  ## ##     ## ##     ##
 *     ######   ##  ##   #### ## ## ## ##     ## ########
 *          ##  ##  ##    ##  ##  #### ##     ## ##
 *    ##    ##  ##  ##    ##  ##   ### ##     ## ##
 *     ######  ####  ######   ##    ##  #######  ##
 */

    /**
     * @api {post} /signup POST
     * @apiVersion     0.1.0
     * @apiName        PostSignup
     * @apiGroup       Signup
     * @apiDescription Allows existing players to become users by connecting an email to their account
     *                 and selecting a unique username. The user can also pick a new persona name to
     *                 be displayed instead of the Steam persona, if they want to.
     *
     * @apiParam {String{4..50}} email    Email address to connect
     * @apiParam {String{3..35}} persona  Persona name, if the same as current, then the persona override is not activated
     * @apiParam {String{3..35}} username A unique, URL friendly username. Only certain symbols are disallowed, therefore usernames like 可愛い or толстый are acceptable
     * @apiParam {String}        steamId  User's Steam ID, to connect the email and username with
     * @apiParam {string}        token    User's token hash
     *
     * @apiUse E_AlreadyExists
     * @apiUse E_BadEmail
     * @apiUse E_BadId
     * @apiUse E_BadMongos
     * @apiUse E_BadPersona
     * @apiUse E_BadRequest
     * @apiUse E_BadToken
     * @apiUse E_BadUsername
     * @apiUse E_EmailTaken
     * @apiUse E_LoginRequired
     * @apiUse E_NameTaken
     * @apiUse E_NameEmailTaken
     * @apiUse E_NotFound
     *
     * @apiSuccess {object} profile Updated profile with new email, username, and persona
     *
     * @apiSuccessExample {json} Success:
     * {
     *   "success": true,
     *   "profile": {..}
     * }
     *
     * @apiErrorExample {json} Error:
     * {
     *   "success": false,
     *   "reason": "NameEmailTaken"
     * }
     */
    router.post('/signup', (req, res) => {
        var post = req.body
        if (typeof post.email    !== 'string' || !post.email    ||
            typeof post.persona  !== 'string' || !post.persona  ||
            typeof post.username !== 'string' || !post.username ||
            typeof post.steamId  !== 'string' || !post.steamId  ||
            typeof post.token    !== 'string' || !post.token)
            return res.json({
                'success': false,
                'reason': BAD_REQUEST
            })

        // Validate some things
        if (!assistant.isSteamId(post.steamId))
            return res.json({
                'success': false,
                'reason': BAD_ID
            })

        if (!assistant.isValidToken({value:post.token,time:Number(new Date())}))
            return res.json({
                'success': false,
                'reason': BAD_TOKEN
            })

        if (!assistant.isEmail(post.email))
            return res.json({
                'success': false,
                'reason': BAD_EMAIL
            })

        post.username = assistant.fixUsername(post.username)
        if (post.username.success)
            post.username = post.username.username
        else
            return res.json({
                'success': false,
                'reason': BAD_USERNAME
            })

        if (!assistant.isPersona(post.persona))
            return res.json({
                'success': false,
                'reason': BAD_PERSONA
            })

        skill.db('collection:Players', (err, db, col) => {
            if (assistant.e(210, err)) {
                db.close()
                return res.json({
                    'success': false,
                    'reason': BAD_MONGOS
                })
            }
            col.find({$or: [
                { 'steam_id': post.steamId },
                { 'username': post.username },
                { 'email': post.email },
                { 'token.value': post.token }
            ]}).toArray((err, docs) => {
                if (assistant.e(211, err)) {
                    db.close()
                    return res.json({
                        'success': false,
                        'reason': BAD_MONGOS
                    })
                }
                var found = false
                var player = {}
                var rname = false
                var remail = false
                for (var i = 0; i < docs.length; i++) {
                    player = docs[i]
                    if (player.username === post.username) rname = true
                    if (player.email    === post.email)    remail = true

                    if (player.steam_id === post.steamId) {
                        if (typeof player.username === 'string' ||
                            typeof player.email === 'string') {
                            db.close()
                            return res.json({
                                'success': false,
                                'reason': ALREADY_EXISTS
                            })
                        }
                        if (player.token.value === post.token &&
                            assistant.isValidToken(player.token)) {
                            found = true
                        } else {
                            db.close()
                            return res.json({
                                'success': false,
                                'reason': LOGIN_REQUIRED
                            })
                        }
                    }

                    if (rname || remail) break
                }

                if (!found || rname || remail) {
                    db.close()
                    return res.json({
                        'success': false,
                        'reason': (rname || remail) ? (rname ? (remail ? 'NameEmailTaken' : NAME_TAKEN) : EMAIL_TAKEN) : NOT_FOUND
                    })
                    // Don't worry about that multi-part one line if-else ;)
                }

                // Found a single account with a matching token, and email+username is available
                var emailtoken = assistant.getToken()
                var ndoc = { $set: {
                    'username': post.username,
                    'persona': post.persona,
                    'email': post.email,
                    'email_verify': emailtoken,
                    'verified': false,
                    'status.level': 'user'
                } }

                // Set a persona override flag, so we know if the persona should be updated when logging in for the future
                if (player.persona !== post.persona)
                    ndoc.$set['status.personaOverride'] = true

                col.findOneAndUpdate({'steam_id': post.steamId}, ndoc, {}, (err, result) => {
                    db.close()
                    if (assistant.e(212, err))
                        return res.json({
                            'success': false,
                            'reason': BAD_MONGOS
                        })

                    mailer.send({
                        to: post.email,
                        from: 'skillbot@csgo-skill.com',
                        subject: 'Activate Your CSGO Skill Account',
                        html: ejs.render(template.email.welcome, {name: post.username, key: emailtoken, email: post.email})
                    })
                      .then(() => { /* Nothing to do here */ })
                      .catch(error => {
                          assistant.e(213, error)
                      })

                    skill.db('collection:Stats', (err, db, col) => {
                        if (assistant.e(214, err)) return // This would really suck
                        ndoc = { 'user': true }
                        assistant.getStatTemplate(null, true).forEach(k => { ndoc['grand.' + k] = 0 })
                        ndoc = {$set: ndoc}
                        col.findOneAndUpdate({'steam_id': post.steamId}, ndoc, {}, (err, _) => {
                            db.close()
                            assistant.e(215, err)
                        })
                    })

                    result.value.username = post.username
                    result.value.email = post.email
                    result.value.persona = post.persona
                    result.value.status.level = 'user'
                    delete result.value._id
                    return res.json({
                        success: true,
                        profile: result.value
                    })
                })
            })
        })
    })


/*
 *     ######   ######## ########    ##     ##  ######  ######## ########
 *    ##    ##  ##          ##       ##     ## ##    ## ##       ##     ##
 *    ##        ##          ##       ##     ## ##       ##       ##     ##
 *    ##   #### ######      ##       ##     ##  ######  ######   ########
 *    ##    ##  ##          ##       ##     ##       ## ##       ##   ##
 *    ##    ##  ##          ##       ##     ## ##    ## ##       ##    ##
 *     ######   ########    ##        #######   ######  ######## ##     ##
 */

     /**
      * @api {get} /user/:id GET
      * @apiVersion     0.1.0
      * @apiName        GetUser
      * @apiGroup       User
      * @apiDescription Retrieves public information about a user's profile
      *
      * @apiParam {String} id  User's Steam ID or Username
      *
      * @apiUse E_BadMongos
      * @apiUse E_NotFound
      *
      * @apiSuccess {object} profile Current profile information
      *
      * @apiSuccessExample {json} Success:
      * {
      *   "success": true,
      *   "profile": {..}
      * }
      *
      * @apiErrorExample {json} Error:
      * {
      *   "success": false,
      *   "reason": "NotFound
      * }
      */

    router.get('/user/:id', (req, res) => {
        var userid = req.params.id.toLowerCase()

        var sendDetails = function (profile) {
            if (res.headersSent) return
            if (!profile) res.json({
                success: false,
                reason: NOT_FOUND
            })
            else res.json({ // We will not return the Steam ID or profileurl intentionally to prevent spam
                success: true,
                user: (profile.status.level === 'user'),
                persona: profile.persona,
                username: profile.username,
                avatar: profile.avatarurl,
                joined: profile.created
            })
        }

        if (userid.length < 3 || userid.length > 35)
            return sendDetails(false)

        // Try searching our DB
        skill.db('collection:Players', (err, db, col) => {
            if (err) {
                db.close()
                console.error(`${req.path}: Error recieved when connecting to MongoDB: ${err}`)
                return res.json({
                    success: false,
                    reason: BAD_MONGOS
                })
            }

            // Always try Steam ID first, prevents users with id-like usernames from impersonating
            if (assistant.isSteamId(userid))
                col.findOne({steam_id: userid}, (err, doc) => {
                    db.close()
                    if (err) {
                        console.error(`${req.path}: Error when searching DB for steam id: ${err}`)
                        if (!res.headersSent) res.json({
                            success: false,
                            reason: BAD_MONGOS
                        })
                        return
                    }
                    sendDetails((doc && doc.steam_id === userid) ? doc : false)
                })
            else
                col.findOne({username: userid}, (err, doc) => {
                    db.close()
                    if (err) {
                        console.error(`${req.path}: Error when searching DB for username: ${err}`)
                        if (!res.headersSent) res.json({
                            success: false,
                            reason: BAD_MONGOS
                        })
                        return
                    }
                    sendDetails((doc && doc.username === userid) ? doc : false)
                })
        })

    })


/*
 *    ########   #######   ######  ########    ##     ##  ######  ######## ########
 *    ##     ## ##     ## ##    ##    ##       ##     ## ##    ## ##       ##     ##
 *    ##     ## ##     ## ##          ##       ##     ## ##       ##       ##     ##
 *    ########  ##     ##  ######     ##       ##     ##  ######  ######   ########
 *    ##        ##     ##       ##    ##       ##     ##       ## ##       ##   ##
 *    ##        ##     ## ##    ##    ##       ##     ## ##    ## ##       ##    ##
 *    ##         #######   ######     ##        #######   ######  ######## ##     ##
 */

    /**
     * @api {post} /user/:id POST
     * @apiVersion     0.1.0
     * @apiName        PostUser
     * @apiGroup       User
     * @apiDescription Updates the user's account information,
     *
     * @apiParam {String} id  Username
     *
     * @apiParam (Post) {String} steamId User's Steam ID
     * @apiParam (Post) {String} token   User's token hash
     * @apiParam (Post) {Object} ops     Profile operations, for changing the email or persona name
     *
     * @apiUse E_BadEmail
     * @apiUse E_BadId
     * @apiUse E_BadPersona
     * @apiUse E_BadRequest
     * @apiUse E_BadToken
     * @apiUse E_BadUsername
     * @apiUse E_LoginRequired
     * @apiUse E_MultipleEmailChanges
     * @apiUse E_NotFound
     *
     * @apiSuccess {object} profile Current profile information
     *
     * @apiSuccessExample {json} Success:
     * {
     *   "success": true,
     *   "profile": {..}
     * }
     *
     * @apiErrorExample {json} Error:
     * {
     *   "success": false,
     *   "reason": "LoginRequired"
     * }
     */

    router.post('/user/:id', (req, res) => {
        /* The reason to require both username and steamId is to further complicate things for hackers
         * trying to mess with accounts. By requiring the correct steam id to be posted to the correct
         * username, it gives users more security since their steam ID is hidden from public APIs,
         * thereby making it harder for casual hackers to mess with the accounts.
         *
         * Obviously, most users have the same persona as on Steam, and they can't currently hide their
         * avatars, so determined hackers would easily discover the steam account. This, however, will
         * be fixed in the future. Likely by having all new accounts start with a default profile
         * picture, which can be toggled to use the steam profile image.
         *
         * For this reason, I'm adding this todo
         */

        // TODO: allow users to hide Steam avatar from the public APIs

        var userid = req.params.id.toLowerCase()

        var post = req.body

        if (typeof post.ops     !== 'object' || !post.ops     ||
            typeof post.steamId !== 'string' || !post.steamId ||
            typeof post.token   !== 'string' || !post.token)
            return res.json({
                'success': false,
                'reason': BAD_REQUEST
            })

        if (userid.length < 3 || userid.length > 35)
            return res.json({
                'success': false,
                'reason': BAD_USERNAME
            })

        // Validate some things
        if (!assistant.isSteamId(post.steamId))
            return res.json({
                'success': false,
                'reason': BAD_ID
            })

        if (!assistant.isValidToken({value:post.token,time:Number(new Date())}))
            return res.json({
                'success': false,
                'reason': BAD_TOKEN
            })

        // Extract the ops into profile changes
        var update = {$set: {}}
        var hasUpdate = false

        if (post.ops.email && typeof post.ops.email === 'string') {
            if (!assistant.isEmail(post.ops.email))
                return res.json({
                    'success': false,
                    'reason': BAD_EMAIL
                })

            update.$set.email = post.ops.email
            update.$set.email_verify = assistant.getToken()
            update.$set.verified = false
            hasUpdate = true
        }

        if (post.ops.persona && typeof post.ops.persona === 'string') {
            if (!assistant.isPersona(post.ops.persona))
                return res.json({
                    'success': false,
                    'reason': BAD_PERSONA
                })

            update.$set.persona = post.ops.persona
            hasUpdate = true
        }

        // Don't allow empty requests
        if (!hasUpdate)
            return res.json({
                'success': false,
                'reason': BAD_REQUEST
            })

        // Try searching our DB
        skill.db('collection:Players', (err, db, col) => {
            if (assistant.e(228, err)) {
                db.close()
                console.error(`${req.path}: Error received when connecting to MongoDB: ${err}`)
                return res.json({
                    success: false,
                    reason: BAD_MONGOS
                })
            }

            col.findOne({steam_id:post.steamId,username:userid}, (error, player) => {
                if (error) {
                    db.close()
                    return res.json({
                        'success': false,
                        'reason': BAD_MONGOS
                    })
                }

                if (player && player.steam_id === post.steamId && player.username === userid) {
                    if (assistant.isValidToken(player.token) &&
                        player.token.value === post.token) {

                        // Check for bad email change requests
                        if (typeof update.$set.email === 'string') {
                            // Disallow verified accounts from changing an unverified email,
                            // unless they are changing it back to the previous email address.
                            if (player.status.changing_email && update.$set.email !== player.status.old_email) {
                                db.close()
                                return res.json({
                                    'success': false,
                                    'reason': MULTIPLE_EMAIL_CHANGES
                                })
                            }

                            // Set verified to true if changing to previous email, which would be verified already
                            // This won't affect unverified accounts, since old_email is only set when the previous
                            // email had been verified
                            if (player.status.changing_email && player.status.old_email && update.$set.email === player.status.old_email) {
                                update.$set.verified = true
                                update.$set['status.changing_email'] = false
                            }

                            // Set these if the player is currently verified
                            if (player.verified) {
                                update.$set['status.old_email'] = player.email
                                update.$set['status.changing_email'] = true
                                update.$set.email_cancel = assistant.getToken()
                            }

                            // For users who haven't yet verified their account, and subsequently won't have
                            // changing_email set, we can safely change the email since they likely just
                            // mistyped it. In those cases, we CANNOT update the token time.
                            if (!player.verified && !player.status.changing_email)
                                update.$set.email_verify.time = player.email_verify.time
                        }

                        // No checks needed for persona, already been validated

                        col.findOneAndUpdate({'steam_id': player.steam_id}, update, (err, _) => {

                            db.close()
                            if (assistant.e(229, err))
                                return res.json({
                                    'success': false,
                                    'reason': BAD_MONGOS
                                })

                            // Do email verification
                            if (typeof update.$set.email === 'string') {

                                // Notify old_email about change
                                if (update.$set['status.old_email'])
                                    mailer.send({
                                        to: update.$set['status.old_email'],
                                        from: 'skillbot@csgo-skill.com',
                                        subject: player.username + ', your email has changed',
                                        html: ejs.render(template.email.emailchangenotify, {newemail: update.$set.email, key: update.$set.email_cancel})
                                    }).then(() => {
                                        /* Nothing to do here */
                                    }).catch(error => {
                                        assistant.e(232, error)
                                    })

                                // Send WELCOME email for unverified, new users
                                if (!player.verified && !player.status.changing_email)
                                    mailer.send({
                                        to: update.$set.email,
                                        from: 'skillbot@csgo-skill.com',
                                        subject: 'Activate Your CSGO Skill Account',
                                        html: ejs.render(template.email.welcome, {name: player.username, key: update.$set.email_verify, email: update.$set.email})
                                    }).then(() => {
                                        /* Nothing to do here */
                                    }).catch(error => {
                                        assistant.e(233, error)
                                    })
                                else mailer.send({
                                    to: update.$set.email,
                                    from: 'skillbot@csgo-skill.com',
                                    subject: player.username + ', verify your new email',
                                    html: ejs.render(template.email.emailchangeactivate, {name: player.username, key: update.$set.email_verify})
                                }).then(() => {
                                    /* Nothing to do here */
                                }).catch(error => {
                                    assistant.e(230, error)
                                })
                            }

                            return res.json({
                                success: true
                            })
                        })
                    } else {
                        db.close()
                        return res.json({
                            'success': false,
                            'reason': LOGIN_REQUIRED
                        })
                    }
                } else {
                    db.close()
                    return res.json({
                        'success': false,
                        'reason': NOT_FOUND
                    })
                }
            })
        })
    })


/*
 *     ######  ########    ###    ########  ######
 *    ##    ##    ##      ## ##      ##    ##    ##
 *    ##          ##     ##   ##     ##    ##
 *     ######     ##    ##     ##    ##     ######
 *          ##    ##    #########    ##          ##
 *    ##    ##    ##    ##     ##    ##    ##    ##
 *     ######     ##    ##     ##    ##     ######
 */

    /**
     * @api {get} /stats/:id User Stats
     * @apiVersion 0.1.0
     * @apiName GetStats
     * @apiGroup Static
     * @apiDescription Returns the given user's public stats. The special user "mega" returns site-wide stats in a unique format.
     *
     * @apiParam {String} id Username
     *
     * @apiUse E_BadMongos
     * @apiUse E_NotFound
     *
     * @apiSuccessExample {json} Success:
     * {
     *   "success": true,
     *   "user": {..},
     *   "stats": {
     *     "current": {..},
     *     "grand": {..},
     *     "history": [
     *       {..},
     *       {..},
     *       ..
     *     ]
     *   }
     * }
     *
     * @apiErrorExample {json} Error:
     * {
     *   "success": false,
     *   "reason": "BadMongos"
     * }
     */

    router.get('/stats/:id', (req, res) => {
        var userid = req.params.id.toLowerCase()

        var sendDetails = function (steamId) {
            if (res.headersSent)
                return

            if (!steamId)
                return res.json({
                    success: false,
                    reason: NOT_FOUND
                })

            skill.db('collection:Stats', (err, db, col) => {

                if (err) {
                    db.close()
                    console.error(`${req.path}: Error when connected to db: ${err}`)
                    if (!res.headersSent) res.json({
                        success: false,
                        reason: BAD_MONGOS
                    })
                    return
                }

                col.findOne({steam_id: steamId}, (err, doc) => {
                    db.close()
                    if (err) {
                        console.error(`${req.path}: Error when searching DB for steam id: ${err}`)
                        if (!res.headersSent) res.json({
                            success: false,
                            reason: BAD_MONGOS
                        })
                        return
                    }

                    if (doc && doc.steam_id === steamId && !res.headersSent) {
                        if (steamId === 'mega')
                            return res.json({
                                success: true,
                                start: doc.start,
                                stats: doc.stats
                            })

                        return res.json({
                            success: true,
                            user: doc.user,
                            stats: {
                                current: doc.current,
                                grand: doc.grand,
                                history: doc.history
                            }
                        })
                    }

                    if (!res.headersSent) res.json({
                        success: false,
                        reason: NOT_FOUND
                    })
                })
            })
        }

        if (userid.length < 3 || userid.length > 35)
            return sendDetails(false)

        if (userid === 'mega')
            return sendDetails('mega')

        // Try searching our DB
        if (assistant.isSteamId(userid))
            sendDetails(userid)
        else skill.db('collection:Players', (err, db, col) => {
            if (err) {
                db.close()
                console.error(`${req.path}: Error recieved when connecting to MongoDB: ${err}`)
                return res.json({
                    success: false,
                    reason: BAD_MONGOS
                })
            }
            col.findOne({username: userid}, (err, doc) => {
                db.close()
                if (err) {
                    console.error(`${req.path}: Error recieved when searching username: ${err}`)
                    return res.json({
                        success: false,
                        reason: BAD_MONGOS
                    })
                }
                sendDetails((doc && doc.username === userid) ? doc.steam_id : false)
            })
        })
    })


    return router
}
