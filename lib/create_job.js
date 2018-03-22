const JOBTYPE = 'update-stats'

//const skill = require('./mongo.js')

require('dotenv').config()
const REDIS_URL = process.env.REDIS_URL
const MONGOURL = process.env.MONGOURL

/*skill.connect(MONGOURL)
skill.db('collection:Stats', (err, col) => {
    if (err) return console.error(err)
    col.findOne({steam_id: '76561198099490962'}, (err, result) => {
        if (err) return console.error(err)
        console.log(result)
    })
})*/

var queue = require('nque').createQueue(REDIS_URL)

queue.createJob(JOBTYPE).run((error, job) => {
    console.log(`Job ${job.id} created for '${JOBTYPE}'`)
})
