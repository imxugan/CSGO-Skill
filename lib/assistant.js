  /*********************************************************
   *    This file is licensed under the MIT 2.0 license    *
   *           Last updated February 23rd, 2018            *
   *   *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *   *
   *    Please check out the full repository located at    *
   *   http://github.com/almic/CSGO-Skill for some other   *
   * important things like the User Agreement and Privacy  *
   *  Policy, as well as some helpful information on how   *
   *     you can contribute directly to the project :)     *
   *********************************************************/

// There are some things that simply require syncronous execution to work :|
const request = require('sync-request')

exports = module.exports

const MONGOURL = process.env.MONGOURL
const STEAMKEY = process.env.STEAMKEY
exports.MONGOURL = MONGOURL
exports.STEAMKEY = STEAMKEY

exports.request = request

exports.consoleResponse = function(output) {
    return '<DOCTYPE html><html><body><script>' +
           `console.log("FLARE-ESPORTS:${output}")` +
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
