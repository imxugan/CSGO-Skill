    /*********************************************************
     *    This file is licensed under the MIT 2.0 license    *
     *             Last updated March 18th, 2018             *
     *   *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *   *
     *    Please check out the full repository located at    *
     *   http://github.com/almic/CSGO-Skill for some other   *
     * important things like the User Agreement and Privacy  *
     *  Policy, as well as some helpful information on how   *
     *     you can contribute directly to the project :)     *
     *********************************************************/
console.log('Booting up...')
var serverStartTime = +new Date()

/* BEGIN MODULES */
const assistant = require('./lib/assistant.js'),
      express = require('express'),
      skill = require('./lib/mongo.js'),
      api = require('./lib/api.js'),
      LightSteamID = require('./lib/openid.js')
console.log(`Libraries loaded in ${+new Date() - serverStartTime}ms`)
/** END MODULES **/

/* BEGIN SETUP */
const app = express()

// Response time logging
app.use((req, res, next) => {
    var start = +new Date()
    res.on('finish', () => { console.log(`Responded to ${req.method} ${req.originalUrl} in ${+new Date() - start}ms`) })
    next()
})
app.use(express.json())
app.use(express.urlencoded({ extended: true }))
app.set('views', './lib/templates')
app.set('view engine', 'ejs')

const MONGOURL = assistant.MONGOURL
const STEAMKEY = assistant.STEAMKEY

try {
    skill.connect(MONGOURL)
    console.log('Mounting API Router')
    app.use(api)
    skill.db('collection:Single', (err, col) => { if (err) throw err;
        col.findOne({name:'test'}, (err, doc) => { if (err) throw err;
            if (doc.test === 'abc123') {
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
    var m = 'Hello World!';
    res.send(m)
})

app.get('/login', (req, res) => {
    var m = '';
    const openid = new LightSteamID('http://www.csgo-skill.com/login', req)
    res.send(m)
})

app.get('/activate', (req, res) => {
    try {
        var get = req.query
        var token = { time: Number(get.t), value: get.k }
        var limit = 2 * 24 * 60 * 60 // 2 day activation link time limit
        if (!assistant.isValidToken(token, limit)) {
            return res.render('activate', {
                page: { title: 'Failed!' },
                message: 'Failed to activate your account. You can try resending the email link.'
            })
        }
        skill.db('collection:Players', (err, col) => {
            if (assistant.e(501, err)) {
                return res.render('activate', {
                    page: { title: 'Failed!' },
                    message: 'Failed to activate your account. There was a server error, try again.'
                })
            }
            col.find({'email-verify': token}).toArray((err, docs) => {
                if (assistant.e(502, err)) {
                    return res.render('activate', {
                        page: { title: 'Failed!' },
                        message: 'Failed to activate your account. There was a server error, try again.'
                    })
                }

                if (docs.length === 1) {
                    // Account is good to go
                    var player = docs[0]
                    var ndoc = {
                        $set: {
                            'verified': true
                        },
                        $unset: {
                            'email-verify': ''
                        }
                    }
                    col.findOneAndUpdate({'steam_id': player.steam_id}, ndoc, (err, result) => {
                        if (assistant.e(503, err)) {
                            return res.render('activate', {
                                page: { title: 'Failed!' },
                                message: 'Failed to activate your account. There was a server error, try again.'
                            })
                        }
                        return res.render('activate', {
                            page: { title: 'Success!' },
                            message: 'You account has been verified. Have fun!'
                        })
                    })
                } else {
                    return res.render('activate', {
                        page: { title: 'Failed!' },
                        message: 'Failed to activate your account. There was a server error, try again.'
                    })
                }
            })
        })
    } catch (e) {
        assistant.e(500, e)
        res.render('activate', {
            page: { title: 'Failed!' },
            message: 'Failed to activate your account. You can try resending the email link.'
        })
    }

})

/** END ROUTING **/

const server = app.listen(process.env.PORT || 8080, () => {
    console.log(`Server Started in ${+new Date() - serverStartTime}ms`)
    console.log(`Listening on port ${server.address().port}`)
})

// Don't forget to change the updated date!
