// Last updated February 26th, 2018

// There are some things that simply require syncronous execution to work :|
const request = require('sync-request')

exports = module.exports

const MONGOURL = process.env.MONGOURL
const STEAMKEY = process.env.STEAMKEY
exports.MONGOURL = MONGOURL
exports.STEAMKEY = STEAMKEY

exports.request = request

exports.e = function(id, error) {
    if (error) {console.error(`Error occured [${id}]: ${error}`); return true}
    return false
}

exports.consoleResponse = function(output) {
    return '<DOCTYPE html><html><body><script>' +
           `console.log("SKILL-BOT:${output}")` +
           `</script><span hidden id="out">${output}</span>` +
           'Greetings, human!</body></html>'
}

exports.downloadStats = function(steamid) {
    try {
        var res = request('GET',
                          'http://api.steampowered.com/ISteamUserStats/GetUserStatsForGame/' +
                          `v2/?appid=730&key=${STEAMKEY}&steamid=${steamid}`)
        res = JSON.parse(res.getBody('utf8'))
        res = { 'success': true, 'res': res }
        return res
    } catch (e) {
        return { 'success': false, 'error': e.message }
    }

}

class Timer {
    constructor(label) {
        this.label = label + ': '
        this.start = +new Date()
    }

    stop() {
        if (this.start === null) {
            console.error('Cannot access a timer that has called stop()')
            return;
        }
        console.log(this.label + ((+new Date()) - this.start) + 'ms')
        this.label = null
        this.start = null
    }
}

exports.timer = function (label = 'Response Time') {
    return new Timer(label)
}
