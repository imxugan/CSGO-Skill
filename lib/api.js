    /*********************************************************
     *    This file is licensed under the MIT 2.0 license    *
     *           Last updated February 26th, 2018            *
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

var apikey, db

module.exports = function api(options) {

    apikey = options.apikey
    skill = options.db

    var router = express.Router()

    router.use((req, res, next) => {
        // Bailout quickly if not in the api subdomain
        if (req.subdomains[0] !== 'api') { return next('router') }
    })

    // From here on, we are in the api subdomain, so there is no need to use next()

    router.get('/login', (req, res) => {
        const openid = new LightSteamID('http://api.csgo-skill.com/login', req)
        if (!openid.mode) {
            res.status(302).location(openid.authUrl)
            res.send()
        } else if(openid.mode == 'cancel') {
            res.send(assistant.consoleResponse(JSON.stringify({
                'success': false,
                'reason': 'auth-canceled'
            })))
        } else {
            if (openid.validate()) {
                var steamid = openid.steam_id
                res.send(`Welcome user ${steamid}`)
            } else if (openid.errno !== 0) {
                assistant.errorOut(openid.errno, openid.error)
                res.send(assistant.consoleResponse(JSON.stringify({
                    'success': false,
                    'reason': `error code ${openid.errno}`
                })))
            } else {
                // Unable to validate. Perhaps reused or fabricated request.
                res.send(`We don't want your chocolate, ${openid.steam_id}!`)
            }
        }
    })

    return router
}
