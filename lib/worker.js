    /*********************************************************
     *    This file is licensed under the MIT 2.0 license    *
     *             Last updated March 19th, 2018             *
     *   *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *   *
     *    Please check out the full repository located at    *
     *   http://github.com/almic/CSGO-Skill for some other   *
     * important things like the User Agreement and Privacy  *
     *  Policy, as well as some helpful information on how   *
     *     you can contribute directly to the project :)     *
     *********************************************************/

console.log('Starting Worker')
var start = +new Date()

const FeedParser = require('feedparser'),
      request = require('request'),
      skill = require('./mongo.js'),
      mailer = require('@sendgrid/mail'),
      ejs = require('ejs'),
      template = require('./template.js')

if (process.env.NODE_ENV !== 'production') {
    require('dotenv').config()
}

const REDIS_URL = process.env.REDIS_URL
const MONGOURL  = process.env.MONGOURL
const STEAMKEY  = process.env.STEAMKEY
const MAILKEY   = process.env.MAILKEY

var queue = require('nque').createQueue(REDIS_URL)
skill.connect(MONGOURL)
skill.db('collection:Single', (err, col) => { if (err) throw err;
    col.findOne({name:'test'}, (err, doc) => { if (err) throw err;
        if (doc.test === 'abc123') {
            console.log(`Successfully connected to MongoDB`)
        }
    })
})
mailer.setApiKey(MAILKEY)

function sendMail(opts) {
    var sub = opts.sub
    var subject = opts.subject
    var content = opts.content
    var from = 'Skill Bot <skillbot@csgo-skill.com>'
    skill.db('collection:Players', (err, col) => {
        if (err) { return console.error(`sendMail to '${sub}' failed with content: ${content.substr(0,50)}`)}
        var ob = {}
        ob['subscription.' + sub] = true
        col.find(ob).toArray((err, docs) => {
            if (err) { return console.error(`sendMail to '${sub}' failed on find(): ${err}`) }
            var to = []
            docs.forEach((doc) => { to.push(doc.email) })
            if (to.length > 0) {
                mailer.sendMultiple({
                    to: to,
                    from: from,
                    subject: subject,
                    html: content
                }).then(() => {
                    mailer.send({
                        to: 'devteam@csgo-skill.com',
                        from: 'Skill Bot <skillbot@csgo-skill.com>',
                        subject: `${to.length} Emails Sent To '${sub}'`,
                        html: `I just emailed <b>${to.length}</b> addresses subscribed to the <b>${sub}</b> mail list.`
                    }).then(() => { /* Nothing to do here */ })
                    .catch(error => { console.error(`sendMail to devteam failed: ${error}`) })
                }).catch(error => {
                    console.error(`sendMail to '${sub}' failed on sendMultiple(): ${error}`)
                })
            }
        })
    })
}

queue.processJob('check-updates', (job, done) => {
    console.log(`Processing job 'check-updates'`)
    var req = request('http://blog.counter-strike.net/index.php/category/updates/feed/')
    var parser = new FeedParser()
    var feed = []
    
    req.on('error', (error) => {
        console.error(`Request error for 'check-updates': ${error}`)
        done(error)
    })
    
    req.on('response', (res) => {
        if (res.statusCode !== 200) {
            req.emit('error', new Error(`Bad status code: ${res.statusCode}`))
        } else {
            req.pipe(parser)
        }
    })
    
    parser.on('error', (error) => {
        console.error(`Parser error for 'check-updates': ${error}`)
        done(error)
    })
    
    parser.on('readable', () => {
        try {
            var item = parser.read()
            if (item !== null) {
                feed.push({
                    title: item.title,
                    link: item.link,
                    time: new Date(item.pubDate),
                    content: item.description
                })
            }
        } catch (e) {
            parser.emit('error', e)
        }
    })
    
    parser.on('end', () => {
        try {
            var self = parser
            skill.db('collection:CSGOUpdates', (err, col) => {
                if (err) { return self.emit('error', err) }
                var check = []
                feed.forEach((item) => {
                    check.push({link: item.link})
                })
                check = { $or: check }
                col.find(check).toArray((err, docs) => {
                    if (err) { return self.emit('error', err) }
                    var toAdd = []
                    var item, found
                    var i, j
                    
                    for (i = 0; i < feed.length; i++) {
                        found = false
                        item = feed[i]
                        for (j = 0; j < docs.length; j++) {
                            if (item.link === docs[j].link) {
                                found = true
                                break
                            }
                        }
                        
                        if (!found) {
                            col.insertOne(item, {}, (err, result) => {
                                if (err) { return self.emit('error', err) }
                                sendMail({
                                    sub: 'csgoupdates',
                                    subject: `CSGO Update: ${item.title}`,
                                    content: ejs.render(template.email.csgoupdate, {item: item})
                                })
                            })
                        }
                    }
                    
                    console.log(`Job 'check-updates' completed successfully`)
                    done()
                    
                })
            })
        } catch (e) {
            parser.emit('error', e)
        }
    })
    
})

var diff = +new Date() - start
console.log(`Worker Started in ${diff}ms`)
