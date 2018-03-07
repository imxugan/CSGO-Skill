    /*********************************************************
     *    This file is licensed under the MIT 2.0 license    *
     *             Last updated March 5th, 2018              *
     *   *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *   *
     *    Please check out the full repository located at    *
     *   http://github.com/almic/CSGO-Skill for some other   *
     * important things like the User Agreement and Privacy  *
     *  Policy, as well as some helpful information on how   *
     *     you can contribute directly to the project :)     *
     *********************************************************/

// This file is meant to contain all API handling, separate from server.js
const express = require('express')
    , assistant = require('./assistant')
    , skill = require('./mongo.js')
    , LightSteamID = require('./openid.js')

const MONGOURL = assistant.MONGOURL
const STEAMKEY = assistant.STEAMKEY

module.exports = api()

function api() {

    var router = express()

    router.on('mount', () => {
        // Ensure mounter has already called skill.connect()
        if (skill.exists()) {
            console.log('API mounted successfully')
        } else {
            throw new Error('Failed to mount API')
        }
    })

    router.all((req, res, next) => {
        // Bailout quickly if not in the api subdomain
        if (req.subdomains[0] !== 'api') { return next('router') }
        next()
    })

    // From here on, we are in the api subdomain, so there is no need to use next()

    router.get('/login', (req, res) => {
        const openid = new LightSteamID('http://api.csgo-skill.com/login', req)
        if (!openid.mode) {
            res.status(302).location(openid.authUrl)
            return res.send()
        } else if(openid.mode == 'cancel') {
            return res.send(assistant.consoleResponse({
                'success': false,
                'reason': 'auth-canceled'
            }))
        } else {
            if (openid.validate()) {
                var steamid = openid.steam_id
                try{
                    skill.db('collection:Players', (err, col) => {
                        if (assistant.e(202, err)) {
                            return res.send(assistant.consoleResponse({
                                'success': false,
                                'reason': 'error code 202'
                            }))
                        }
                        col.find({'steam_id': steamid}).toArray((err, docs) => {
                            if (assistant.e(203, err)) {
                                return res.send(assistant.consoleResponse({
                                    'success': false,
                                    'reason': 'error code 203'
                                }))
                            }
                            var handled = false
                            if (docs.length === 1) {
                                var player = docs[0]
                                if (typeof player.verified === 'boolean' && !player.verified) {
                                    // Notify player to complete verification
                                    player.success = true
                                    player.notify = 'email-verification'
                                    delete player._id
                                    return res.send(assistant.consoleResponse(player))
                                } else if (typeof player.verified === 'boolean') {
                                    player.success = true
                                    delete player._id
                                    return res.send(assistant.consoleResponse(player))
                                } else if (typeof player.token.value === 'string') {
                                    if (assistant.isValidToken(player.token)) {
                                        player.success = true
                                        delete player._id
                                        return res.send(assistant.consoleResponse(player))
                                    }
                                    handled = true
                                    var token = assistant.getToken()
                                    col.findOneAndUpdate({'steam_id': steamid},
                                        {$set: { 'token': token }}, (err, result) => {
                                        if (assistant.e(206, err)) {
                                            return res.send(assistant.consoleResponse({
                                                'success': false,
                                                'reason': 'error code 206'
                                            }))
                                        }
                                        result.value.success = true
                                        result.value.token = token
                                        delete result.value._id
                                        return res.send(assistant.consoleResponse(result.value))
                                    })
                                }
                                // In any other case it should simply be ignored and allowed to
                                // fall through to the stuff after the following else if.
                            } else if (docs.length !== 0){
                                // Strangly, two or more accounts came back. Just error out
                                return res.send(assistant.consoleResponse({
                                    'success': false,
                                    'reason': 'error code 204'
                                }))
                            }

                            if (!handled) {
                                // New account? Check and add it!
                                var valid = assistant.validateAccount(steamid)
                                if (!valid.success) {
                                    return res.send(assistant.consoleResponse({
                                        'success': false,
                                        'reason': valid.reason
                                    }))
                                }
                                var doc = valid.profile
                                col.insertOne(doc, {}, (err, result) => {
                                    if (assistant.e(205, err)) {
                                        return res.send(assistant.consoleResponse({
                                            'success': false,
                                            'reason': 'error code 205'
                                        }))
                                    }
                                    doc.success = true
                                    return res.send(assistant.consoleResponse(doc))
                                })
                            }
                        })
                    })
                } catch (e) {
                    try {
                        return res.send(assistant.consoleResponse({
                            'success': false,
                            'reason': 'error code 200'
                        }))
                    } catch (e) {
                        // Error resulted from response, just log it
                        return assistant.e(201, e)
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
        }
    })

    router.post('/signup', (req, res) => {
        var post = req.body
        if (typeof post.email === 'string' && post.email &&
            typeof post.persona === 'string' && post.persona &&
            typeof post.username === 'string' && post.username &&
            typeof post.steamid === 'string' && post.steamid &&
            typeof post.token === 'string' && post.token) {

            // Validate some things
            if (!assistant.isSteamId(post.steamid)) {
                return res.send(assistant.consoleResponse({
                    'success': false,
                    'reason': 'bad-steamid'
                }))
            }
            if (!assistant.isEmail(post.email)) {
                return res.send(assistant.consoleResponse({
                    'success': false,
                    'reason': 'bad-email'
                }))
            }
            post.username = assistant.fixUsername(post.username)
            if (post.username.success) {
                post.username = post.username.username
            } else {
                return res.send(assistant.consoleResponse({
                    'success': false,
                    'reason': 'bad-username'
                }))
            }

            skill.db('collection:Players', (err, col) => {
                if (assistant.e(300, err)) {
                    return res.send(assistant.consoleResponse({
                        'success': false,
                        'reason': 'error code 300'
                    }))
                }
                col.find({$or: [
                    { 'steam_id': post.steamid },
                    { 'username': post.username },
                    { 'email': post.email },
                    { 'token': { 'value': post.token } }
                ]}).toArray((err, docs) => {
                    if (assistant.e(301, err)) {
                        return res.send(assistant.consoleResponse({
                            'success': false,
                            'reason': 'error code 301'
                        }))
                    }
                    var found = false
                    var player = {}
                    var account = {}
                    var rname = false
                    var remail = false
                    for (var i = 0; i < docs.length; i++) {
                        player = docs[i]
                        if (player.username == post.username) { rname = true }
                        if (player.email == post.email) { remail = true }
                        if (player.steam_id === post.steamid) {
                            if (typeof player.username === 'string' ||
                                typeof player.email === 'string') {
                                return res.send(assistant.consoleResponse({
                                    'success': false,
                                    'reason': 'already-exists'
                                }))
                            }
                            if (player.token.value === post.token &&
                                assistant.isValidToken(player.token)) {
                                found = true
                                account = player
                            } else {
                                return res.send(assistant.consoleResponse({
                                    'success': false,
                                    'reason': 'login'
                                }))
                            }
                        }
                    }

                    if (!found) {
                        return res.send(assistant.consoleResponse({
                            'success': false,
                            'reason': (rname || remail) ? (rname ? ('name-in-use' + (remail ? ',email-in-use' : '')) : 'email-in-use') : 'not-found'
                        }))
                        // Don't worry about that multi-part one line if-else ;)
                    }

                    // Found a single account with a matching token, and email+username is available
                    var docupdate = { $set: {
                        'username': post.username,
                        'email': post.email,
                        'persona': post.persona
                    }}
                    col.findOneAndUpdate({'steam_id': post.steamid}, docupdate, {}, (err, result) => {
                        if (assistant.e(303, err)) {
                            return res.send(assistant.consoleResponse({
                                'success': false,
                                'reason': 'error code 303'
                            }))
                        }
                        result.value.success = true
                        result.value.username = post.username
                        result.value.email = post.email
                        result.value.persona = post.persona
                        delete result.value._id
                        return res.send(assistant.consoleResponse(result.value))
                    })
                })
            })
        } else {
            return res.send(assistant.consoleResponse({
                'success': false,
                'reason': 'error code 302'
            }))
        }
    })

    return router
}
