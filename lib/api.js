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

                                if (!player.verified) {
                                    // Notify player to complete verification
                                    return res.send(assistant.consoleResponse({
                                        'success': true,
                                        'email': player.email,
                                        'steamid': steamid,
                                        'secret': player.secret,
                                        'status': player.status,
                                        'notify': 'email-verification'
                                    }))
                                } else {
                                    return res.send(assistant.consoleRequest({
                                        'success': true,
                                        'email': player.email,
                                        'steamid': steamid,
                                        'secret': player.secret,
                                        'status': player.status
                                    }))
                                }
                            } else if (docs.length !== 0){
                                // Strangly, two or more accounts came back. Just error out
                                return res.send(assistant.consoleResponse({
                                    'success': false,
                                    'reason': 'error code 204'
                                }))
                            }
                            // New account? Check it!
                            var valid = validateAccount(steamid)
                            if (!valid.success) {
                                return res.send(assistant.consoleResponse({
                                    'success': false,
                                    'reason': valid.reason
                                }))
                            }
                            return res.send(assistant.consoleResponse(valid))
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

/**
 * Given a steamid, this function get's the player summaries and returns an object with `success`
 * true and some other data points if the account meets all of the following requirements. Otherwise
 * it returns a list of the requirements it does not meet.
 *
 * - Public profile
 * - Owns CS: GO
 * - Has 10 hours played (600 minutes)
 *
 * Object for successful validation has all of these:
 *
 * - persona
 * - profileurl
 * - avatarurl
 * - steamid
 */
function validateAccount(steamid) {
    var summary = assistant.downloadSummaries(steamid)
    if (!summary.success) {
        assistant.e(206, summary.error)
        return { 'success': false, 'reason': 'error code 206' }
    }

    if (summary.res.response.players[0].steamid !== steamid) {
        return { 'success': false, 'reason': 'failed to validate account' }
    }

    var player = summary.res.response.players[0]

    if (player.communityvisibilitystate !== 3) {
        return { 'success': false, 'reason': 'private' }
    }

    var games = assistant.downloadOwnsCSGO(steamid)
    if (!games.success) {
        assistant.e(207, games.error)
        return { 'success': false, 'reason': 'error code 207' }
    }

    if (games.res.response.game_count !== 1) {
        return { 'success': false, 'reason': 'own' }
    }

    if (games.res.response.games[0].playtime_forever < 600) {
        return { 'success': false, 'reason': 'playtime' }
    }

    // Good to go!
    return {
        'success': true,
        'persona': player.personaname,
        'profileurl': player.profileurl,
        'avatarurl': player.avatarfull,
        'steamid': steamid
    }

}
