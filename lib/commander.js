// This is just a helpful file for myself, so I can run Mongo commands if I ever need to

require('dotenv').config()

const skill = require('./mongo.js')

const MONGOURL  = process.env.MONGOURL
skill.connect(MONGOURL)

skill.db('collection:CSGOUpdates', (err, db, col) => {
    col.updateMany({}, {$rename: {"time": "published"}}, (err, result) => {
        console.log(result.ok)
        db.close()
    })
})
