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

/* It seems that the cron module has some alarming quality issues, mainly circulating around DST and
 * a glitch which causes it to fire events thousands of times simultaneously. Numerous people have
 * complained about it, and the "solution" proposed seems to degrade speed and increase CPU usage a
 * lot more than it should.
 *
 * Until these issues are fixed, using cron is a temporary solution and should be replaced as soon
 * as possible. Currently I've implemented a test which connects to the MongoDB and verifies that
 * enough time has passed between calls before doing anything. At least this way if the thousand
 * call bug happens, it will melt our MongoDB instead of causing more serious issues with workers.
 */

console.log('Starting Clock')
var start = +new Date()

const CronJob = require('cron').CronJob,
      skill = require('./mongo.js')

if (process.env.NODE_ENV !== 'production') {
    require('dotenv').config()
}

const REDIS_URL = process.env.REDIS_URL
const MONGOURL = process.env.MONGOURL

skill.connect(MONGOURL)
skill.db('collection:Single', (err, col) => { if (err) throw err;
    col.findOne({name:'test'}, (err, doc) => { if (err) throw err;
        if (doc.test === 'abc123') {
            console.log(`Successfully connected to MongoDB`)
        }
    })
})

var queue = require('nque').createQueue(REDIS_URL)

/** CRON JOBS **/

var interval

const TIMEWINDOW = 2 * 60 * 1000 // 2 minute execution window

// The MongoDB execution time checker
var doJobCheck = function(type, interval, callback) {
    skill.db('collection:Single', (e, c) => {
        if (e) return console.error(`Job '${type}' failed check: ${e}`)
        c.findOne({name:'jobs'}, (e, d) => {
            if (e) return console.error(`Job '${type}' failed check: ${e}`)
            if ( (+new Date() - d[type]) > (interval - TIMEWINDOW) ) {
                var ob = {}
                ob[type] = +new Date()
                col.updateOne({name:'jobs'}, {$set: ob}, (e) => {
                    if (e) return console.error(`Job '${type}' failed time update: ${e}`)
                    callback()
                })
            }
        })
    })
}

// CSGO Update checker
try {
    interval = 30 * 60 * 1000 // Every 30 minutes
    var c1 = new CronJob({
        cronTime: '0 0,30 * * * *',
        onTick: () => {
            doJobCheck('check-updates', interval, () => {
                queue.createJob('check-updates').run()
            })
        },
        onComplete: () => {
            console.log(`Cron job 'check-updates' was run`)
        },
        startNow: true,
        timeZone: 'Europe/London' // For UTC time
    })
} catch (error) {
    console.error(`Clock Error 100: ${error}`)
}
