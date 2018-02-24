  /********************************************************
  *    This file is licensed under the MIT 2.0 license    *
  *           Last updated February 23rd, 2018            *
  *   *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *   *
  *    Please check out the full repository located at    *
  *   http://github.com/almic/CSGO-Skill for some other   *
  * important things like the User Agreement and Privacy  *
  *  Policy, as well as some helpful information on how   *
  *     you can contribute directly to the project :)     *
  ********************************************************/

/* BEGIN MODULES */
const assistant = require('./lib/assistant.js')
var startup = assistant.timer('Server Started')
const express = require('express')
    , mongojs = require('mongojs')
    , LightSteamID = require('./lib/openid.js')

/** END MODULES **/

/* BEGIN SETUP */
const app = express()
app.use(express.json())
app.use(express.urlencoded({ extended: true }))

const MONGOURL = assistant.MONGOURL
const STEAMKEY = assistant.STEAMKEY
var skilldb = null
var hasDb = false

try {
    skilldb = mongojs(MONGOURL)
    hasDb = true
} catch (e) {
    assistant.errorOut(10, e.message)
}

/** END SETUP **/


/* BEING ROUTING */
app.get('/', (req, res) => {
    var timer = assistant.timer()
    var m = 'Hello World!';
    skilldb.Example.find((err, docs) => {
        m += `\nDoc located, _id: ${docs[0]._id}, test: ${docs[0].test}`
        res.send(m)
        timer.stop()
    })
    //res.send(m)
})

app.get('/login', (req, res) => {
    var m = '';
    var timer = assistant.timer()
    if (req.subdomains[0] === 'api') {
        // Device request
        const openid = new LightSteamID('http://api.csgo-skill.com/login', req)
        if (!openid.mode) {
            res.status(302).location(openid.authUrl)
            res.send()
            timer.stop()
        } else if(openid.mode == 'cancel') {
            m += 'canceled'
        } else {
            if (openid.validate()) {
                var steamid = openid.steam_id
                m += steamid
            } else if (openid.errno !== 0) {
                assistant.errorOut(openid.errno, openid.error)
                m += 'Error occured during validation'
            }
        }
    } else {
        // Website request
        const openid = new LightSteamID('http://www.csgo-skill.com/login', req)
    }

    if (!res.headersSent) {
        res.send(m)
        timer.stop()
    }
})
/** END ROUTING **/

const server = app.listen(process.env.PORT || 8080, () => {
    startup.stop()
    console.log('Listening on port ' + server.address().port)

})

// Don't forget to change the updated date!
