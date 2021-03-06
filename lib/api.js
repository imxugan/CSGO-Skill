    /*********************************************************
     *    This file is licensed under the MIT 2.0 license    *
     *              Last updated May 31st, 2018              *
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

    router.all('/version', (req, res) => {
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

    // Allow steamid & token login, if the account token is still valid, and reset the token
    router.post('/login', (req, res) => {
        var post = req.body
        if (typeof post.steamid !== 'string' || !post.steamid ||
            typeof post.token !== 'string' || !post.token)
            return res.json({
                'success': false,
                'reason': 'bad-request'
            })

        if (!assistant.isSteamId(post.steamid))
            return res.json({
                'success': false,
                'reason': 'bad-steamid'
            })

        if (!assistant.isValidToken({value:post.token,time:Number(new Date())}))
            return res.json({
                'success': false,
                'reason': 'bad-token'
            })

        skill.db('collection:Players', (error, db, col) => {
            if (assistant.e(200, error)) {
                db.close()
                return res.json({
                    'success': false,
                    'reason': 'error code 200'
                })
            }
            col.findOne({steam_id:post.steamid}, (error, player) => {
                if (assistant.e(224, error)) {
                    db.close()
                    return res.json({
                        'success': false,
                        'reason': 'error code 224'
                    })
                }
                if (player && player.steam_id === post.steamid) {
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
                                    'reason': 'error code 225'
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
                            'reason': 'login-required'
                        })
                    }
                } else {
                    db.close()
                    return res.json({
                        'success': false,
                        'reason': 'not-found'
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

    router.get('/login', (req, res) => {
        const openid = new LightSteamID('http://api.csgo-skill.com/login', req)
        if (!openid.mode) {
            res.status(302).location(openid.authUrl)
            return res.send()
        }
        if (openid.mode === 'cancel')
            return res.send(assistant.consoleResponse({
                'success': false,
                'reason': 'auth-canceled'
            }))

        if (openid.validate()) {
            var steamid = openid.steam_id
            try {
                skill.db('collection:Players', (err, db, col) => {
                    if (assistant.e(201, err)) {
                        db.close()
                        return res.send(assistant.consoleResponse({
                            'success': false,
                            'reason': 'error code 201'
                        }))
                    }
                    col.find({'steam_id': steamid}).toArray((err, docs) => {
                        if (assistant.e(202, err)) {
                            db.close()
                            return res.send(assistant.consoleResponse({
                                'success': false,
                                'reason': 'error code 202'
                            }))
                        }

                        if (docs.length === 1) {
                            var player = docs[0]
                            delete player._id

                            // Notify player to complete verification
                            if (typeof player.verified === 'boolean' && !player.verified)
                                player.notify = 'email-verify'

                            // Update the token and return
                            var token = assistant.getToken()
                            player.token = token
                            col.findOneAndUpdate({'steam_id': steamid},
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
                                'reason': 'error code 204'
                            }))
                        }

                        // New account, check and add it!
                        var valid = assistant.validateAccount(steamid)
                        if (!valid.success) {
                            db.close()
                            return res.send(assistant.consoleResponse(valid))
                        }
                        col.insertOne(valid.profile, {}, (err, _) => {
                            db.close()
                            if (assistant.e(205, err))
                                return res.send(assistant.consoleResponse({
                                    'success': false,
                                    'reason': 'error code 205'
                                }))

                            skill.db('collection:Stats', (err, db, col) => {
                                if (assistant.e(206, err)) return
                                col.insertOne(assistant.getStatTemplate(steamid), {}, (err, _) => {
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
                        'reason': 'error code 208'
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
                'reason': `error code ${openid.errno}`
            }))
        } else {
            // Unable to validate. Perhaps reused or fabricated request.
            return res.send(assistant.consoleResponse({
                'success': false,
                'reason': `We don't want your chocolate, ${openid.steam_id}!`
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

    router.post('/signup', (req, res) => {
        var post = req.body
        if (typeof post.email === 'string' && post.email &&
            typeof post.persona === 'string' && post.persona &&
            typeof post.username === 'string' && post.username &&
            typeof post.steamid === 'string' && post.steamid &&
            typeof post.token === 'string' && post.token) {

            // Validate some things
            if (!assistant.isSteamId(post.steamid))
                return res.json({
                    'success': false,
                    'reason': 'bad-steamid'
                })

            if (!assistant.isValidToken({value:post.token,time:Number(new Date())}))
                return res.json({
                    'success': false,
                    'reason': 'bad-token'
                })

            if (!assistant.isEmail(post.email))
                return res.json({
                    'success': false,
                    'reason': 'bad-email'
                })

            post.username = assistant.fixUsername(post.username)
            if (post.username.success)
                post.username = post.username.username
            else
                return res.json({
                    'success': false,
                    'reason': 'bad-username'
                })

            if (!assistant.isPersona(post.persona))
                return res.json({
                    'success': false,
                    'reason': 'bad-persona'
                })

            skill.db('collection:Players', (err, db, col) => {
                if (assistant.e(210, err)) {
                    db.close()
                    return res.json({
                        'success': false,
                        'reason': 'error code 210'
                    })
                }
                col.find({$or: [
                    { 'steam_id': post.steamid },
                    { 'username': post.username },
                    { 'email': post.email },
                    { 'token.value': post.token }
                ]}).toArray((err, docs) => {
                    if (assistant.e(211, err)) {
                        db.close()
                        return res.json({
                            'success': false,
                            'reason': 'error code 211'
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
                        if (player.steam_id === post.steamid) {
                            if (typeof player.username === 'string' ||
                                typeof player.email === 'string') {
                                db.close()
                                return res.json({
                                    'success': false,
                                    'reason': 'already-exists'
                                })
                            }
                            if (player.token.value === post.token &&
                                assistant.isValidToken(player.token)) {
                                found = true
                            } else {
                                db.close()
                                return res.json({
                                    'success': false,
                                    'reason': 'login'
                                })
                            }
                        }
                    }

                    if (!found || rname || remail) {
                        db.close()
                        return res.json({
                            'success': false,
                            'reason': (rname || remail) ? (rname ? ('name-in-use' + (remail ? ',email-in-use' : '')) : 'email-in-use') : 'not-found'
                        })
                        // Don't worry about that multi-part one line if-else ;)
                    }

                    // Found a single account with a matching token, and email+username is available
                    var emailtoken = assistant.getToken()
                    var ndoc = { $set: {
                        'username': post.username,
                        'persona': post.persona,
                        'email': post.email,
                        'email-verify': emailtoken,
                        'verified': false,
                        'status.level': 'user'
                    } }
                    col.findOneAndUpdate({'steam_id': post.steamid}, ndoc, {}, (err, result) => {
                        db.close()
                        if (assistant.e(212, err))
                            return res.json({
                                'success': false,
                                'reason': 'error code 212'
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
                            col.findOneAndUpdate({'steam_id': post.steamid}, ndoc, {}, (err, _) => {
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
        } else {
            return res.json({
                'success': false,
                'reason': 'error code 216'
            })
        }
    })


/*
 *    ########  ########   #######  ######## #### ##       ########       ##    ##     ##  ######  ######## ########
 *    ##     ## ##     ## ##     ## ##        ##  ##       ##            ##     ##     ## ##    ## ##       ##     ##
 *    ##     ## ##     ## ##     ## ##        ##  ##       ##           ##      ##     ## ##       ##       ##     ##
 *    ########  ########  ##     ## ######    ##  ##       ######      ##       ##     ##  ######  ######   ########
 *    ##        ##   ##   ##     ## ##        ##  ##       ##         ##        ##     ##       ## ##       ##   ##
 *    ##        ##    ##  ##     ## ##        ##  ##       ##        ##         ##     ## ##    ## ##       ##    ##
 *    ##        ##     ##  #######  ##       #### ######## ######## ##           #######   ######  ######## ##     ##
 */

    router.get(['/profile/:id', '/user/:id'], (req, res) => {
        var userid = req.params.id.toLowerCase()

        var sendDetails = function (profile) {
            if (res.headersSent) return
            if (!profile) res.json({
                success: false,
                reason: 'not-found'
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
                    reason: 'error code 217'
                })
            }

            // Always try SteamID first, prevents users with id-like usernames from impersonating
            if (assistant.isSteamId(userid))
                col.findOne({steam_id: userid}, (err, doc) => {
                    db.close()
                    if (err) {
                        console.error(`${req.path}: Error when searching DB for steam id: ${err}`)
                        if (!res.headersSent) res.json({
                            success: false,
                            reason: 'error code 218'
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
                            reason: 'error code 219'
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
         * For this reason, I'm adding
         * TODO: allow users to hide Steam avatar from the public APIs
         */

        var userid = req.params.id.toLowerCase()

        var post = req.body

        if (typeof post.ops === 'object' && post.ops &&
            typeof post.steamid === 'string' && post.steamid &&
            typeof post.token === 'string' && post.token) {

            if (userid.length < 3 || userid.length > 35)
                return res.json({
                    'success': false,
                    'reason': 'not-found'
                })

            // Validate some things
            if (!assistant.isSteamId(post.steamid))
                return res.json({
                    'success': false,
                    'reason': 'bad-steamid'
                })

            if (!assistant.isValidToken({value:post.token,time:Number(new Date())}))
                return res.json({
                    'success': false,
                    'reason': 'bad-token'
                })

            // Extract the ops into profile changes
            var update = {$set: {}}
            var hasUpdate = false

            if (post.ops.email && typeof post.ops.email === 'string') {
                if (!assistant.isEmail(post.ops.email))
                    return res.json({
                        'success': false,
                        'reason': 'bad-email'
                    })

                update.$set.email = post.ops.email
                update.$set['email-verify'] = assistant.getToken()
                update.$set.verified = false
                hasUpdate = true
            }

            if (post.ops.persona && typeof post.ops.persona === 'string') {
                if (!assistant.isPersona(post.ops.persona))
                    return res.json({
                        'success': false,
                        'reason': 'bad-persona'
                    })

                update.$set.persona = post.ops.persona
                hasUpdate = true
            }

            // Don't allow empty requests
            if (!hasUpdate)
                return res.json({
                    'success': false,
                    'reason': 'bad-request'
                })

            // Try searching our DB
            skill.db('collection:Players', (err, db, col) => {
                if (assistant.e(228, err)) {
                    db.close()
                    console.error(`${req.path}: Error received when connecting to MongoDB: ${err}`)
                    return res.json({
                        success: false,
                        reason: 'error code 228'
                    })
                }

                col.findOne({steam_id:post.steamid,username:userid}, (error, player) => {
                    if (error) {
                        db.close()
                        return res.json({
                            'success': false,
                            'reason': 'error code 231'
                        })
                    }

                    if (player && player.steam_id === post.steamid && player.username === userid) {
                        if (assistant.isValidToken(player.token) &&
                            player.token.value === post.token) {

                            // Check for bad email change requests
                            if (typeof update.$set.email === 'string') {
                                // Disallow verified accounts from changing an unverified email,
                                // unless they are changing it back to the previous email address.
                                if (player.oldemail && typeof player['email-verify'] === 'object' && update.$set.email !== player.oldemail) {
                                    db.close()
                                    return res.json({
                                        'success': false,
                                        'reason': 'multiple-email-changes'
                                    })
                                }

                                // Set verified to true if changing to previous email, which would be verified already
                                // This won't affect unverified accounts, since oldemail is only set when the previous
                                // email had been verified
                                if (player.oldemail && update.$set.email === player.oldemail)
                                    update.$set.verified = true

                                // Set oldemail if the player is currently verified
                                if (player.verified) {
                                    update.$set.oldemail = player.email
                                    update.$set['email-cancel'] = assistant.getToken()
                                }

                                // For users who haven't yet verified their account, and subsequently won't have
                                // oldemail set on the account, we can safely change the email since they likely
                                // have just mistyped it. In those cases, we CANNOT update the email token.
                                if (!player.verified && typeof player.oldemail !== 'string')
                                    update.$set['email-verify'] = player['email-verify']
                            }

                            // No checks needed for persona, already been validated

                            col.findOneAndUpdate({'steam_id': player.steam_id}, update, (err, _) => {

                                db.close()
                                if (assistant.e(229, err))
                                    return res.json({
                                        'success': false,
                                        'reason': 'error code 229'
                                    })

                                // Do email verification
                                if (typeof update.$set.email === 'string') {

                                    // Notify oldemail about change
                                    if (typeof update.$set.oldemail === 'string')
                                        mailer.send({
                                            to: update.$set.oldemail,
                                            from: 'skillbot@csgo-skill.com',
                                            subject: player.username + ', your email has changed',
                                            html: ejs.render(template.email.emailchangenotify, {newemail: update.$set.email, key: update.$set['email-cancel']})
                                        }).then(() => {
                                            /* Nothing to do here */
                                        }).catch(error => {
                                            assistant.e(232, error)
                                        })

                                    // Send WELCOME email for unverified, new users
                                    if (!player.verified && typeof player.oldemail !== 'string')
                                        mailer.send({
                                            to: update.$set.email,
                                            from: 'skillbot@csgo-skill.com',
                                            subject: 'Activate Your CSGO Skill Account',
                                            html: ejs.render(template.email.welcome, {name: player.username, key: player['email-verify'], email: update.$set.email})
                                        }).then(() => {
                                            /* Nothing to do here */
                                        }).catch(error => {
                                            assistant.e(233, error)
                                        })
                                    else mailer.send({
                                        to: update.$set.email,
                                        from: 'skillbot@csgo-skill.com',
                                        subject: player.username + ', verify your new email',
                                        html: ejs.render(template.email.emailchangeactivate, {name: player.username, key: update.$set['email-verify']})
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
                                'reason': 'login-required'
                            })
                        }
                    } else {
                        db.close()
                        return res.json({
                            'success': false,
                            'reason': 'not-found'
                        })
                    }
                })
            })
        } else {
            return res.json({
                'success': false,
                'reason': 'bad-request'
            })
        }
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

    router.get('/stats/:id', (req, res) => {
        var userid = req.params.id.toLowerCase()

        var sendDetails = function (steamId) {
            if (res.headersSent) return
            if (!steamId) return res.json({ success: false, reason: 'not-found' })

            skill.db('collection:Stats', (err, db, col) => {

                if (err) {
                    db.close()
                    console.error(`${req.path}: Error when connected to db: ${err}`)
                    if (!res.headersSent) res.json({
                        success: false,
                        reason: 'error code 220'
                    })
                    return
                }

                col.findOne({steam_id: steamId}, (err, doc) => {
                    db.close()
                    if (err) {
                        console.error(`${req.path}: Error when searching DB for steam id: ${err}`)
                        if (!res.headersSent) res.json({
                            success: false,
                            reason: 'error code 221'
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
                        reason: 'not-found'
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
                    reason: 'error code 222'
                })
            }
            col.findOne({username: userid}, (err, doc) => {
                db.close()
                if (err) {
                    console.error(`${req.path}: Error recieved when searching username: ${err}`)
                    return res.json({
                        success: false,
                        reason: 'error code 223'
                    })
                }
                sendDetails((doc && doc.username === userid) ? doc.steam_id : false)
            })
        })
    })


    return router
}
