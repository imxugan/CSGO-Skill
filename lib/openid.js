/**
 * Thanks to this repository:
 *     https://github.com/iignatov/LightOpenID
 *
 * Thank you for using the MIT license, Mewp!
 *
 * This only works for this project's usage case. Since we will only ever use
 * Steam's OpenID login, it is greatly compressed and has a lot of hard-coded
 * variables. This makes our OpenID process super fast, and very specific.
 *
 * Feel free to reuse this file for your own use. I just ran the requests that
 * the original PHP class does, and pulled the final auth Url from it.
 */

const request = require('sync-request')
const querystring = require('querystring')

exports = module.exports = class LightSteamID {

    constructor(host, req) {
        this.error = null;
        this.errno = 0;
        this.data = req.query;
        this.returnUrl = host;

        this.root = '';
        var offset = host.indexOf('://')
        this.root += (offset === -1) ? (req.secure ? 'https://' : 'http://') : '';
        offset = (offset !== -1) ? offset + 3 : 0;
        var end = host.indexOf('/', offset)
        this.root += (end === -1) ? host : host.substring(0, end);

        this.hostname = 'https://steamcommunity.com';
        this.loginpath = '/openid/login';
    }

    get authUrl() {
        return this.hostname + this.loginpath +
               '?openid.ns=http%3A%2F%2Fspecs.openid.net%2Fauth%2F2.0' +
               '&openid.mode=checkid_setup' +
               '&openid.return_to=' + encodeURIComponent(this.returnUrl) +
               '&openid.realm=' + encodeURIComponent(this.root) +
               '&openid.ns.sreg=http%3A%2F%2Fopenid.net%2Fextensions%2Fsreg%2F1.1' +
               '&openid.claimed_id=http%3A%2F%2Fspecs.openid.net%2Fauth%2F2.0%2Fidentifier_select' +
               '&openid.identity=http%3A%2F%2Fspecs.openid.net%2Fauth%2F2.0%2Fidentifier_select';
    }

    get mode() {
        return (typeof(this.data['openid.mode']) === undefined) ? null : this.data['openid.mode'];
    }

    validate() {
        try {
            if (this.mode !== 'id_res') {
                this.error = `Incorrect validation mode "${this.mode}", expected "id_res"`;
                this.errno = 1;
                return false;
            }

            if (this.data['openid.return_to'] != this.returnUrl) {
                this.error = `Incorrect return url "${this.data['openid.return_to']}", expected "${this.returnUrl}"`;
                this.errno = 2;
                return false;
            }

            this.steam_id = this.data['openid.claimed_id'];
            this.steam_id = this.steam_id.substring(this.steam_id.indexOf('/id/') + 4);

            var params = {'openid.mode': ''}
            var signed = this.data['openid.signed'].split(',');
            signed.forEach((item) => {
                params['openid.' + item] = this.data['openid.' + item];
            });

            params['openid.mode'] = 'check_authentication'
            const options = {
                body: querystring.stringify(params),
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded'
                }
            };
            
            console.log(options)

            // This would be blocking anyway, since we can't give a response until
            // we know the result of the validation. Async doesn't help here.
            try {
                var res = request('POST', this.hostname + this.loginpath, options);
                res = res.getBody('utf8');
                console.log(res)
                return (res.search(/is_valid\s*:\s*true/i) !== -1);
            } catch (e) {
                this.error = `OpenID validation request failed. Error: ${e.message}`;
                this.errno = 3;
                return false;
            }
        } catch (e) {
            console.error(e.message)
        }
        return false
    }

}
