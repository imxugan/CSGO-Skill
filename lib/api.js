    /*********************************************************
     *    This file is licensed under the MIT 2.0 license    *
     *           Last updated March 2nd, 2018            *
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
        //if (req.subdomains[0] !== 'api') { return next('router') }
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
                            if (docs.length === 1) {
                                var player = docs[0]
                                if (typeof player.verified === 'boolean' && !player.verified) {
                                    // Notify player to complete verification
                                    player.success = true
                                    player.notify = 'email-verification'
                                    return res.send(assistant.consoleResponse(player))
                                } else if (typeof player.verified === 'boolean') {
                                    player.success = true
                                    return res.send(assistant.consoleResponse(player))
                                } else if (typeof player.token.value === 'string') {
                                    if (assistant.isValidToken(player.token)) {
                                        player.success = true
                                        return res.send(assistant.consoleResponse(player))
                                    }
                                    col.findOneAndUpdate({'steam_id': steamid},
                                        {$set: {
                                            'token': {
                                                'time': +new Date(),
                                                'value': assistant.getToken()
                                            }
                                        }}, (err, result) => {
                                        if (assistant.e(206, err)) {
                                            return res.send(assistant.consoleResponse({
                                                'success': false,
                                                'reason': 'error code 206'
                                            }))
                                        }
                                        result.value.success = true
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
                            // New account? Check and reserve it!
                            var valid = assistant.validateAccount(steamid)
                            if (!valid.success) {
                                return res.send(assistant.consoleResponse({
                                    'success': false,
                                    'reason': valid.reason
                                }))
                            }
                            var doc = {
                                'steam_id': valid.steamid,
                                'token': {
                                    'time': +new Date(),
                                    'value': assistant.getToken()
                                },
                                'persona': valid.persona,
                                'profileurl': valid.profileurl,
                                'avatarurl': valid.avatarurl
                            }
                            col.insertOne(doc, {}, (err, result) => {
                                if (assistant.e(205, err)) {
                                    return res.send(assistant.consoleResponse({
                                        'success': false,
                                        'reason': 'error code 205'
                                    }))
                                }
                                doc.success = true
                                res.send(assistant.consoleResponse(doc))
                            })

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

    return router
}
