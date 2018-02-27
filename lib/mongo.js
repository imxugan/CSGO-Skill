// Last updated February 26th, 2018

var mongodb = require('mongodb')
var _connection = function() { throw new Error('Connection not set! Call connect() first!') }
var dbname = ''
var url = ''

module.exports = {
    connect: function(url, options, callback) {
        dbname = url.substr(url.lastIndexOf('/') + 1)
        url = url.substring(0, url.lastIndexOf('/') + 1) // Include the slash in url
        _connection = function (name, cb) {
            mongodb.connect(url + (name || dbname), options, (err, client) => {
                if (err) { cb(err) } else { cb(null, client.db((name || dbname))) }
            })
        }
        if (typeof callback === 'function') { _connection(callback) }
        return true
    },
    db: function(name, callback) {
        if (typeof name === 'function') { _connection(null, name) }
        else if (typeof name === 'string') {
            // Collection shortcut
            if (name.indexOf('collection:') === -1) { _connection(name, callback) }
            else {
                if (typeof callback !== 'function') {
                    throw new Error(`Callback must be provided for 'collection:' calls!`)
                }
                _connection(null, (err, db) => {
                    if (err) { callback(err) }
                    else { db.collection(name.substr(11), {strict: true}, callback) }
                })
            }
        } else { console.log(`Invalid db() call. No callback provided. Connection not opened`) }
    }
}
