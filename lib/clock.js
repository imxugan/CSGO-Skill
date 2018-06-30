    /*********************************************************
     *    This file is licensed under the MIT 2.0 license    *
     *              Last updated June 6th, 2018              *
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
var start = Number(new Date())

const CronJob = require('cron').CronJob
const skill = require('./mongo.js')

if (process.env.NODE_ENV !== 'production')
    require('dotenv').config()

const REDIS_URL = process.env.REDIS_URL
const MONGOURL = process.env.MONGOURL

skill.connect(MONGOURL)
skill.db('collection:Single', (err, db, col) => {
    if (err) throw err

    col.findOne({name:'test'}, (err, doc) => {
        if (err) throw err

        db.close()
        if (doc.test === 'abc123')
            console.log('Successfully connected to MongoDB')
    })
})

var queue = require('nque').createQueue(REDIS_URL)

/** CRON JOBS **/

const TIMEWINDOW = 2 * 60 * 1000 // 2 minute execution window

// The MongoDB execution time checker
var doJobCheck = function (type, interval, callback) {
    skill.db('collection:Single', (e, db, c) => {
        if (e) {
            db.close()
            return console.error(`Job '${type}' failed check: ${e}`)
        }
        c.findOne({name:'jobs'}, (e, d) => {
            if (e) {
                db.close()
                return console.error(`Job '${type}' failed check: ${e}`)
            }
            var timeDiff = (Number(new Date()) - d[type]) - (interval - TIMEWINDOW)
            if (timeDiff >= 0) {
                var ob = {}
                ob[type] = Number(new Date())
                c.updateOne({name:'jobs'}, {$set: ob}, e => {
                    db.close()
                    if (e)
                        return console.error(`Job '${type}' failed time update: ${e}`)

                    callback()
                })
            } else {
                db.close()
                console.log(`Job '${type}' denied creation, missed interval by ${Math.abs(timeDiff)}ms`)
            }
        })
    })
}

// CSGO Update checker
var checkUpdates = function () {
    var interval = 30 * 60 * 1000 // Every 30 minutes
    var start = Number(new Date())
    doJobCheck('check-updates', interval, () => {
        console.log('Creating job \'check-updates\'')
        queue.createJob('check-updates').run((error, job) => {
            console.log(`Job ${job.id} created for 'check-updates' in ${Number(new Date()) - start}ms`)
        })
    })
}

// Stat Updater
var updateStats = function () {
    var interval = 24 * 60 * 60 * 1000 // Every day
    var start = Number(new Date())
    doJobCheck('update-stats', interval, () => {
        console.log('Creating job \'update-stats\'')
        queue.createJob('update-stats').run((error, job) => {
            console.log(`Job ${job.id} created for 'update-stats' in ${Number(new Date()) - start}ms`)
        })
    })
}

// Upon restart, check to see if we missed any intervals
console.log('Checking for missed intervals')
checkUpdates()
updateStats()

// CSGO Update checker
try {
    // eslint-disable-next-line no-unused-vars
    var c1 = new CronJob({
        cronTime: '0 0,30 * * * *',
        onTick: () => {
            console.log('Ticking \'check-updates\'')
            checkUpdates()
        },
        onComplete: () => {
            console.error('Cron job \'check-updates\' was stopped')
        },
        startNow: true,
        timeZone: 'UTC'
    })
} catch (error) {
    console.error(`Clock Error 100: ${error}`)
}

// Stat Updater
try {
    // eslint-disable-next-line no-unused-vars
    var c2 = new CronJob({
        cronTime: '0 0 0 * * *',
        onTick: () => {
            console.log('Ticking \'update-stats\'')
            updateStats()
        },
        onComplete: () => {
            console.error('Cron job \'update-stats\' was stopped')
        },
        startNow: true,
        timeZone: 'UTC'
    })
} catch (error) {
    console.error(`Clock Error 101: ${error}`)
}

var diff = Number(new Date()) - start
console.log(`Clock Started in ${diff}ms`)
