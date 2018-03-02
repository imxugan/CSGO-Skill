// Last updated March 2nd, 2018

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
    output = JSON.stringify(output)
    return '<DOCTYPE html><html><body><script>' +
           `console.log(\`SKILL-BOT:${output}\`)` +
           `</script><span hidden id="out">${output}</span>` +
           'Greetings, human!</body></html>'
}

exports.downloadOwnsCSGO = function(steamid) {
    try {
        var res = request('GET',
                          'http://api.steampowered.com/IPlayerService/GetOwnedGames/' +
                          `v0001/?key=${STEAMKEY}&input_json={%22steamid%22:${steamid},` +
                          '%22appids_filter%22:[730]}')
        res = JSON.parse(res.getBody('utf8'))
        return { 'success': true, 'res': res }
    } catch (e) {
        return { 'success': false, 'error': e }
    }
}

exports.downloadSummaries = function(steamid) {
    try {
        var res = request('GET',
                          'http://api.steampowered.com/ISteamUser/GetPlayerSummaries/' +
                          `v0002/?key=${STEAMKEY}&steamids=${steamid}`)
        res = JSON.parse(res.getBody('utf8'))
        return { 'success': true, 'res': res }
    } catch (e) {
        return { 'success': false, 'error': e }
    }
}

exports.downloadStats = function(steamid) {
    try {
        var res = request('GET',
                          'http://api.steampowered.com/ISteamUserStats/GetUserStatsForGame/' +
                          `v2/?appid=730&key=${STEAMKEY}&steamid=${steamid}`)
        res = JSON.parse(res.getBody('utf8'))
        return { 'success': true, 'res': res }
    } catch (e) {
        return { 'success': false, 'error': e }
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
