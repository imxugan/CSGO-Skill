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
console.log('Booting up...')

/* BEGIN MODULES */
const assistant = require('./lib/assistant.js')
var startup = assistant.timer('Server Started')

console.log('Loading libraries')
const express = require('express')
    , skill = require('./lib/mongo.js')
    , api = require('./lib/api.js')
    , LightSteamID = require('./lib/openid.js')
console.log('Loading complete')
/** END MODULES **/

/* BEGIN SETUP */
const app = express()
app.use(express.json())
app.use(express.urlencoded({ extended: true }))

const MONGOURL = assistant.MONGOURL
const STEAMKEY = assistant.STEAMKEY

try {
    skill.connect(MONGOURL)
    console.log('Mounting API')
    app.use(api)
    skill.db('collection:Testing', (err, col) => { if (err) throw err;
        col.find().toArray((err, docs) => { if (err) throw err;
            if (docs[0].test === 'abc123') {
                console.log(`Successfully connected to MongoDB`)
            }
        })
    })
} catch (e) {
    assistant.e(100, e)
    throw e // rethrow because we shouldn't start the server if anything in the try{} failed.
}

/** END SETUP **/


/* BEING ROUTING */
app.get('/', (req, res) => {
    var timer = assistant.timer()

    var m = 'Hello World!';
    res.send(m)

    timer.stop()
})

app.get('/login', (req, res) => {
    var timer = assistant.timer()

    var m = '';
    const openid = new LightSteamID('http://www.csgo-skill.com/login', req)

    res.send(m)

    timer.stop()
})
/** END ROUTING **/

const server = app.listen(process.env.PORT || 8080, () => {
    startup.stop()
    console.log('Listening on port ' + server.address().port)
})

// Don't forget to change the updated date!
