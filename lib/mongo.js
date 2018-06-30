// Last updated March 2nd, 2018

/* eslint-disable */

var mongodb = require('mongodb')
var _connection = function() { throw new Error('Connection not set! Call connect() first!') }
var _dbname = ''
var _url = ''
var _options = {}
var _exists = false

module.exports = {
    connect: function(url, options, callback) {
        if (typeof options === 'object') {
            _options = options
        } else if (typeof options === 'function') {
            callback = options
        }
        if (typeof url === 'string') {
            _dbname = url.substr(url.lastIndexOf('/') + 1)
            _url = url.substring(0, url.lastIndexOf('/') + 1) // Include the slash in url
            _exists = true
        } else if (typeof url === 'function') {
            callback = url
        } else if (typeof url === 'object') {
            _options = url
        }
        _connection = function (name, cb) {
            mongodb.connect(_url + (name || _dbname), (options || _options), (err, client) => {
                if (err) { cb(err, client) } else { cb(null, client, client.db((name || _dbname))) }
            })
        }
        if (typeof callback === 'function') { _connection(null, callback) }
        return true
    },
    db: function(name, callback) {
        if (typeof name === 'function') { _connection(null, name) }
        else if (typeof name === 'string') {
            // Collection shortcut
            if (name.indexOf('collection:') === -1) {
                if (typeof callback !== 'function') { callback = function() {} }
                _connection(name, callback)
            } else {
                if (typeof callback !== 'function') {
                    throw new Error(`Callback must be provided for 'collection:' calls!`)
                }
                _connection(null, (err, client, db) => {
                    if (err) { callback(err, client, db) }
                    else {
                        db.collection(name.substr(11), {strict: true}, (err, col) => {
                            callback(err, client, col)
                        })
                    }
                })
            }
        } else { console.log(`Invalid db() call. No callback provided. Connection not opened`) }
    },
    exists: function () { return _exists }
}
