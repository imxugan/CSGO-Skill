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
const express = require('express')
    , mongojs = require('mongojs')
    , LightSteamID = require('./lib/openid.js')
    , assistant = require('./lib/assistant.js')

/** END MODULES **/

/* BEGIN SETUP */
const app = express()
app.use(express.json())
app.use(express.urlencoded({ extended: true }))

const MONGOURL = assistant.MONGOURL
const STEAMKEY = assistant.STEAMKEY

try {
    const skilldb = mongojs(MONGOURL)
    skilldb.Example.find((err, docs) => {
        console.log(docs)
    });
} catch (e) {
    assistant.errorOut(10, e.message)
}

/** END SETUP **/


/* BEING ROUTING */
app.get('/', (req, res) => {
    var m = 'Hello World!';
    skilldb.Example.find((err, docs) => {
        m += `\nDoc located, _id: ${docs[0]._id}, test: ${docs[0].test}`
    })
    res.send(m)
})

app.get('/login', (req, res) => {
    var m = '';
    if (req.subdomains[0] === 'api') {
        // Device request
        const openid = new LightSteamID('http://api.csgo-skill.com/login', req)
        if (!openid.mode) {
            res.location(openid.authUrl)
        } else if(openid.mode == 'cancel') {
            m += 'canceled'
        } else {
            if (openid.validate()) {
                var steamid = openid.steam_id
                m += steamid
            } else if (openid.errno !== 0) {
                m += `Error occured during validation: [${openid.errno}] ${openid.error}`
            }
        }
    } else {
        // Website request
        const openid = new LightSteamID('http://www.csgo-skill.com/login', req)
    }

    res.send(m)
})
/** END ROUTING **/

const server = app.listen(process.env.PORT || 8080, () => {
    console.log('Listening on port ' + server.address().port)

})

// Don't forget to change the updated date!
