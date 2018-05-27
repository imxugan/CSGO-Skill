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

console.log('Booting up...')
const serverStartTime = Number(new Date())

/* Modules */
const express = require('express')

/* Custom Libs */
const assistant = require('./lib/assistant.js')
    , skill = require('./lib/mongo.js')
    , api = require('./lib/api.js')
//    , LightSteamID = require('./lib/openid.js')

console.log(`Libraries loaded in ${Number(new Date()) - serverStartTime}ms`)

/* BEGIN SETUP */
const app = express()

// Response time logging
app.use((req, res, next) => {
    const start = Number(new Date())
    res.on('finish', () => {
        console.log(`Responded to ${req.method} ${req.originalUrl} in ${Number(new Date()) - start}ms`)
    })
    next()
})
app.use(express.json())
app.use(express.urlencoded({extended: true}))
app.set('views', './lib/templates')
app.set('view engine', 'ejs')

const MONGOURL = assistant.MONGOURL
//const STEAMKEY = assistant.STEAMKEY

try {
    skill.connect(MONGOURL)
    console.log('Mounting API Router')
    app.use(api)
    skill.db('collection:Single', (err, db, col) => {
        if (err) throw err

        col.findOne({name: 'test'}, (err, doc) => {
            if (err) throw err

            db.close()
            if (doc.test === 'abc123')
                console.log('Successfully connected to MongoDB')

        })
    })
} catch (e) {
    assistant.e(100, e)
    throw e // Rethrow error, we shouldn't start the server if anything failed
}

/** END SETUP **/

/* BEING ROUTING */
app.get('/', (req, res) => {
    const m = 'Hello World!'
    res.send(m)
})


/*
 *       ###     ######  ######## #### ##     ##    ###    ######## ########
 *      ## ##   ##    ##    ##     ##  ##     ##   ## ##      ##    ##
 *     ##   ##  ##          ##     ##  ##     ##  ##   ##     ##    ##
 *    ##     ## ##          ##     ##  ##     ## ##     ##    ##    ######
 *    ######### ##          ##     ##   ##   ##  #########    ##    ##
 *    ##     ## ##    ##    ##     ##    ## ##   ##     ##    ##    ##
 *    ##     ##  ######     ##    ####    ###    ##     ##    ##    ########
 */

app.get('/activate', (req, res) => {

    var basicerror = 'Failed to activate your account. There was a server error, try again.'

    try {
        var get = req.query
        var token = { time: Number(get.t), value: get.k }
        var limit = 24 * 60 * 60 // 24 hour activation link time limit

        if (!assistant.isValidToken(token, limit))
            return res.render('activate', {
                page: { title: 'Failed!' },
                message: 'Failed to activate your account. Looks like your link has expired! You can try resending the email link.'
            })

        skill.db('collection:Players', (err, db, col) => {
            if (assistant.e(501, err)) {
                db.close()
                return res.render('activate', {
                    page: { title: 'Failed!' },
                    message: basicerror
                })
            }
            col.find({'email-verify': token}).toArray((err, docs) => {
                if (assistant.e(502, err)) {
                    db.close()
                    return res.render('activate', {
                        page: { title: 'Failed!' },
                        message: basicerror
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
                    col.findOneAndUpdate({'steam_id': player.steam_id}, ndoc, (err, _) => {
                        db.close()
                        if (assistant.e(503, err))
                            return res.render('activate', {
                                page: { title: 'Failed!' },
                                message: basicerror
                            })

                        return res.render('activate', {
                            page: { title: 'Success!' },
                            message: 'You account has been verified. Have fun!'
                        })
                    })
                } else {
                    db.close()
                    return res.render('activate', {
                        page: { title: 'Failed!' },
                        message: basicerror
                    })
                }
            })
        })
    } catch (e) {
        assistant.e(500, e)
        res.render('activate', {
            page: { title: 'Failed!' },
            message: 'Failed to activate your account. Something crazy happened, you can try refreshing the page.'
        })
    }
})

/*
 *     ######     ###    ##    ##  ######  ######## ##
 *    ##    ##   ## ##   ###   ## ##    ## ##       ##
 *    ##        ##   ##  ####  ## ##       ##       ##
 *    ##       ##     ## ## ## ## ##       ######   ##
 *    ##       ######### ##  #### ##       ##       ##
 *    ##    ## ##     ## ##   ### ##    ## ##       ##
 *     ######  ##     ## ##    ##  ######  ######## ########
*/

app.get('/cancel', (req, res) => {

    var basicerror = 'Unable to cancel the email change! Please try again, if you keep getting this message then please contact the devteam@csgo-skill.com to report this issue.'

    try {
        var get = req.query
        var token = { time: Number(get.t), value: get.k }
        var limit = 24 * 60 * 60 // 24 hour cancel link time limit

        if (!assistant.isValidToken(token, limit))
            return res.render('activate', {
                page: { title: 'Failed!' },
                message: 'Failed to activate your account. Looks like your link has expired! You need to contact the devteam@csgo-skill.com and <b>report that your account has been hijacked</b>. We cannot guarantee that we\'ll be able to help you if you fail to report this within a week after the email change was requested.'
            })

        skill.db('collection:Players', (err, db, col) => {
            if (assistant.e(504, err)) {
                db.close()
                return res.render('activate', {
                    page: { title: 'Failed!' },
                    message: basicerror
                })
            }
            col.find({'email-cancel': token}).toArray((err, docs) => {
                if (assistant.e(505, err)) {
                    db.close()
                    return res.render('activate', {
                        page: { title: 'Failed!' },
                        message: basicerror
                    })
                }

                if (docs.length === 1) {
                    // Account is good to go
                    var player = docs[0]
                    var update = {
                        $set: {
                            'email': player.oldemail,
                            'canceled-email': player.email
                        },
                        $unset: {
                            'email-verify': ''
                        }
                    }

                    if (typeof player.oldemail !== 'string' || !assistant.isEmail(player.oldemail)) {
                        db.close()
                        assistant.e(508, new Error(`A user with a valid cancel link had either no 'oldemail' or a bad 'oldemail'! Steam ID: ${player.steam_id}`))
                        return res.render('activate', {
                            page: { title: 'Failed!' },
                            message: basicerror
                        })
                    }

                    col.findOneAndUpdate({'steam_id': player.steam_id}, update, (err, _) => {
                        db.close()
                        if (assistant.e(506, err))
                            return res.render('activate', {
                                page: { title: 'Failed!' },
                                message: basicerror
                            })

                        return res.render('activate', {
                            page: { title: 'Success!' },
                            message: `You have successfully canceled the activation of the new email address (${player.email}), and reverted back to your previous email (${player.oldemail}).<br><br>If you never requested this change in the first place, you might consider verifying the security of your Steam Account, as it may have been hijacked.<br><br>If you haven't already done so, please logout and log back into your CSGO Skill account on any device, which will block any further unauthorized access to your CSGO Skill account.`
                        })
                    })
                } else {
                    db.close()
                    return res.render('activate', {
                        page: { title: 'Failed!' },
                        message: basicerror
                    })
                }
            })
        })
    } catch (e) {
        assistant.e(507, e)
        res.render('activate', {
            page: { title: 'Failed!' },
            message: 'Failed to activate your account. Something crazy happened, you can try refreshing the page.'
        })
    }
})


/*
 *    ##        #######   ######   #### ##    ##
 *    ##       ##     ## ##    ##   ##  ###   ##
 *    ##       ##     ## ##         ##  ####  ##
 *    ##       ##     ## ##   ####  ##  ## ## ##
 *    ##       ##     ## ##    ##   ##  ##  ####
 *    ##       ##     ## ##    ##   ##  ##   ###
 *    ########  #######   ######   #### ##    ##
 */

app.get('/login', (req, res) => {
    const m = ''
    //const openid = new LightSteamID('http://www.csgo-skill.com/login', req)
    res.send(m)
})

/** END ROUTING **/

const server = app.listen(process.env.PORT || 8080, () => {
    console.log(`Server Started in ${Number(new Date()) - serverStartTime}ms`)
    console.log(`Listening on port ${server.address().port}`)
})

// TODO Don't forget to change the updated date!
