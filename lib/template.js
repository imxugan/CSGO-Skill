    /*********************************************************
     *    This file is licensed under the MIT 2.0 license    *
     *             Last updated March 10th, 2018             *
     *   *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *   *
     *    Please check out the full repository located at    *
     *   http://github.com/almic/CSGO-Skill for some other   *
     * important things like the User Agreement and Privacy  *
     *  Policy, as well as some helpful information on how   *
     *     you can contribute directly to the project :)     *
     *********************************************************/

// The use of syncronous file reading is intentional, as these are loaded before the server starts

const fs = require('fs')

function read(path) {
    return fs.readFileSync(path, 'utf8')
}

exports = module.exports

console.log('Loading Templates')

exports.email = {
    welcome: read('./lib/templates/email/welcome.ejs'),
    csgoupdate: read('./lib/templates/email/csgoupdate.ejs')
}

console.log('Templates Loaded')
