<?php

/**
 * This file must be included at the time when a new account should be built
 * and only takes two external variables: $steamID, $create [OPTIONAL]
 *
 * If `isset($create) == true`, we also expect to have an $email and $name.
 * Otherwise, we simply check that the account COULD be created.
 */

/**** REQUIREMENTS ****
 * A public profile
 * Owns CS: GO
 * Has 10 hours played (36000 seconds)
 * Scored 200 kills
 * Has the achievement Newb World Order (Win 10 rounds) [WIN_ROUNDS_LOW]
 */

if (!isset($steamID)) {
    error_log("Missing \$steamID for included file \"makeAccount\"!");
    die("Missing steamID!");
}

function check($steamID, $doubleCheck = false) {
    $url = "http://api.steampowered.com/ISteamUser/GetPlayerSummaries/" .
    "v0002/?key=" . $STEAMKEY . "&steamids=" . $steamID;
    $json= json_decode(file_get_contents($url));

    if (!isset($json->repsonse->players[0]->communityvisibilitystate)) {
        // Non-existent profile
        if ($doubleCheck) { return NULL; }
        consoleExit("{\"success\":false,\"error\":\"1320\"}");
    }

    $player = $json->response->players[0];

    if ($steamID !== $player->steamid) {
        // Provided steamID is not properly sanitized or contains extra (attack) data, stop now
        error_log("1323 - Possible SQL injection attack detected. \$steamID = \"" . $steamID . "\"");
        if ($doubleCheck) { return NULL; }
        consoleExit("{\"success\":false,\"error\":\"1323\"}");
    }

    if ($player->communityvisibilitystate !== 3) {
        // Profile is not visible
        if ($doubleCheck) { return NULL; }
        consoleExit("{\"success\":false,\"error\":\"1321\"}");
    }

    $url = "http://api.steampowered.com/ISteamUserStats/GetUserStatsForGame/" .
           "v2/?appid=730&key=" . $STEAMKEY . "&steamid=" . $steamID;
    $json = json_decode(file_get_contents($url));

    if (!isset($json->playerstats)) {
        // Profile does not have CS: GO
        if ($doubleCheck) { return NULL; }
        consoleExit("{\"success\":false,\"error\":\"13221\"}");
    }

    $stats = $json->playerstats->stats;
    $achvs = $json->playerstats->achievements;
    $goodPlaytime = false;
    $goodKills = false;
    $goodAchv = false;
    $chkPlaytime = false;
    $chkKills = false;

    foreach ($stats as $e) {
        if ($e->name === "total_time_played") {
            if ($e->value > 36000) {
                $goodPlaytime = true;
            }
            $chkPlaytime = true;
        } else if ($e->name === "total_kills") {
            if ($e->value > 200) {
                $goodKills = true;
            }
            $chkKills = true;
        }
        if ($chkPlaytime && $chkKills) { break; }
    } unset($e);

    foreach ($achvs as $a) {
        if ($a->name === "WIN_ROUNDS_LOW") {
            if ($a->value === 1) {
                $goodAchv = true;
                break;
            }
        }
    } unset($a);

    if (!$goodPlaytime || !$goodAchv || !$goodKills) {
        // Profile does not meet extra requirements
        if ($doubleCheck) { return NULL; }
        consoleExit("{\"success\":false,\"error\":\"13222\"}");
    }

    if ($doubleCheck) { return $stats; }

    $conn = mysqli_connect($server, $username, $password, $flaredb);
    if ($conn->connect_error) {
        error_log("1312 - Failed to connect to MySQL Database '" . $flaredb .
        "' with error (" . $conn->connect_errno . "): " . $conn->connect_error);
        consoleExit("{\"success\":false,\"error\":\"1312\"}");
    }

    $steamID = $conn->real_escape_string($steamID);

    $query = "SELECT `id` FROM `Players_01` WHERE `steamid`=\"" . $steamID .
    "\" AND `verified` = 1 AND NOT `secret`=\"empty\"";
    $result = $conn->query($query);

    if ($result->num_rows !== 0) {
        // Steam ID is already in use, cannot create profile.
        $conn->close();
        consoleExit("{\"success\":false,\"error\":\"13241\"}");
    }

    $query = "SELECT `id` FROM `Players_01` WHERE `steamid`=\"" . $steamID .
    "\" AND `verified` = 0 AND `secret`=\"empty\"";
    $result = $conn->query($query);

    if ($result->num_rows === 1) {
        /**
         * Steam ID is reserved already. Due to logic flow and the possibility
         * that the user simply lost their verify code, it is safe to assume the
         * user is legit and we can SAFELY delete the row so that the following
         * INSERT works correctly.
         */
        $query = "DELETE FROM `Players_01` WHERE `steamid`=\"" . $steamID .
        "\" AND `verified` = 0 AND `secret`=\"empty\"";
        if (!$conn->query($query)) {
            error_log("13261 - The SQL query to delete a reserved account row failed. Here's what we got: " . $conn->error_list);
            $conn->close();
            consoleExit("{\"success\":false,\"error\":\"13261\"}");
        }
    }

    // Only checking if account COULD be made, and it can :)

    /**
     * To make the next call faster and more secure, we'll "reserve" the given
     * Steam ID in our Players table, that way we don't have to waste time
     * checking every account twice, nor risk account hijacking.
     *
     * We'll also set a secret to verify the next call is still the same user.
     * If they lose it, no big deal, they should still be able to try again.
     */
    $string = substr(hash("sha256", random_bytes(200)), 0, 10);
    $status = array(
        "level" => "member",
        "ban_history" => array(),
        "verify" => $string
    );
    $query = "INSERT INTO `Players_01` (`steamid`, `status`) VALUES (\"" . $steamID .
             "\", " . json_encode(json_encode($status)) . ")";
    if ($conn->query($query)) {
        $conn->close();
        consoleExit("{\"success\":true,\"verify\":\"" . $string . "\"}");
    } else {
        error_log("13262 - The SQL query to reserve user accuont failed. Here's what we got: " . $conn->error_list);
        $conn->close();
        consoleExit("{\"success\":false,\"error\":\"13262\"}");
    }
}

// Being Main Stuff
if (!isset($create)) {
    check($steamID);
} else if (isset($email) && isset($name) && isset($verify)){

    $conn = mysqli_connect($server, $username, $password, $flaredb);
    if ($conn->connect_error) {
        error_log("1312 - Failed to connect to MySQL Database '" . $flaredb .
        "' with error (" . $conn->connect_errno . "): " . $conn->connect_error);
        consoleExit("{\"success\":false,\"error\":\"1312\"}");
    }

    $steamID = $conn->real_escape_string($steamID);
    /* This query is our ultimate test to make sure an account is reserved, but
       not yet fully created. */
    $query = "SELECT `id`, `status` FROM `Players_01` WHERE `steamid` = \"" . $steamID .
    "\" AND `verified` = 0 AND `secret` = \"empty\"";
    $result = $conn->query($query);

    if ($result->num_rows === 0) {
        /**
         * Cannot create account due to the reserved row not being found. The
         * existence of the reserved row, according to our logic flow, means
         * that the Steam account specified in the row is owned by someone who
         * has access to it.
         *
         * This script only creates the reserved row immediately following a
         * successful OpenID verification. Calling it without $create and not
         * immediately following OpenID verification is a security risk, such
         * that anyone could create an account with their own email and someone
         * else's Steam account, thus "stealing" an account (hooray for them?)
         *
         * Technically, a newly reserved account is very fragile. Anyone could
         * hijack an account if they do it right after it's reserved. But thanks
         * to the $verify, the possibility of this is slightly reduced.
         *
         * The reason for the TRY AGAIN error is because this error should
         * technically never be seen if the user was going through the login
         * flow as we designed it. The user would literally have to send a
         * POST request to the addAccount script directly, or change hidden
         * form data to attempt to create an account under a different user.
         *
         * In the UI, this error is regarded just like a normal error, except
         * it should only output "Try and do that again, please."
         */
        $conn->close();
        error_log("!!! - Account with Steam ID \"" . substr($steamID, 0, 25) .
        "\" creation attempted without a reserved row. Potentially attempted " .
        "outside of defined logic flow.");
        consoleExit("{\"success\":false,\"error\":\"TRY AGAIN\"}");
    }

    // Grab `id` and `status` for things
    $result = $result->fetch_assoc();
    $id = $result["id"];
    $status = json_decode($result["status"], true);

    if ($status["verify"] !== $verify) {
        // Given $verify doesn't match. Cannot validate request!
        $conn->close();
        consoleExit("{\"success\":false,\"error\":\"1323\"}");
    }

    $name = $conn->real_escape_string(substr($name, 0, 30));

    // Check that name is at least 3 chars, and email is 5-50 chars
    //             (a@b.c - anextremelylongemailthatnooneuses@bigdomain.domain)
    if (strlen($name) < 3 || strlen($email) < 5 || strlen($email) > 50){
        $conn->close();
        consoleExit("{\"success\":false,\"error\":\"1325\"}");
    }

    // Check that email does not contain strange non-email characters
    $invalids = str_split("'\"/;:[]{}~+=!#$%^&*()");
    foreach ($invalids as $char){
        if (strpos($email, $char) !== false){
            $conn->close();
            consoleExit("{\"success\":false,\"error\":\"1325\"}");
        }
    }

    // Validity check
    $email = filter_var($email, FILTER_SANITIZE_EMAIL);
    if (!filter_var($email, FILTER_VALIDATE_EMAIL)){
        $conn->close();
        consoleExit("{\"success\":false,\"error\":\"1325\"}");
    }

    $query = "SELECT `id` FROM `Players_01` WHERE `username` = \"" . $name . "\"";
    $result = $conn->query($query);
    if ($result->num_rows !== 0) {
        /**
         * Username is already in use, they should pick another. Remind them
         * that they may create a persona name later, but the username must
         * still be unique.
         */
        $conn->close();
        consoleExit("{\"success\":false,\"error\":\"13242\"}");
    }

    $query = "SELECT `id` FROM `Players_01` WHERE `email` = \"" . $conn->real_escape_string($email) . "\"";
    $result = $conn->query($query);
    if ($result->num_rows !== 0) {
        /**
         * Email is in use, please be sure to ask the user if they want to login
         * using that email. Let them know they can change their connected Steam
         * account if they want!
         */
        $conn->close();
        consoleExit("{\"success\":false,\"error\":\"13243\"}");
    }

    /**
     * Okay so we've vetted everything we need to in all the ways we want to.
     * Here's a quick run-down of everything we know:
     *   - Username is 3-30 characters
     *   - Email is 5-50 characters
     *   - Email is highly likely valid
     *   - Username is not already being used
     *   - Email is not already being used
     *   - SteamID already exists in DB as a reserved row, i.e NOT AN ACCOUNT
     *
     * Commence account creation at once!
     */

    $stats = check($steamID, true); // Double check the account
    if (is_null($stats)) {
        // Account is no longer valid (for some reason), I cry everytime :'(
        $conn->close();
        consoleExit("{\"success\":false,\"error\":\"13223\"}");
    }

    // Generate secret
    $string = hash("sha512", random_bytes(200));

    /* this adds random dashes in the hash id, to make it slightly more
       secure and effectively make it impossible to guess a user's id */
    $first = 0;
    $next = random_int(8,12);
    $secret = substr($string, 0, $next);
    $o_string = $secret;
    while (strlen($o_string) != strlen($string)){
        $first += $next;
        $next = random_int(6,24);
        $secret .= "-" . substr($string, $first, $next);
        $o_string .= substr($string, $first, $next);
    }
    $secret = $id . "-" . $secret; // This is 100% unique all the time

    // Make activation link
    $status["link"] = array(
        "secret" => substr(hash("512", random_bytes(200)), 0, 20),
        "action" => "verify"
    );

    // Grab the Data template
    require_once("data_template");

    $query = "UPDATE `Players_01` SET " .
    "`username`=\"" . $name . "\"," .
    "`persona`=\"" . $name . "\"," .
    "`email`=\"" . $email . "\"," .
    "`secret`=\"" . $secret . "\"," .
    "`current`=" . json_encode(json_encode($stats)) . // Double encoding escapes the things
    ",`data`=" . json_encode(json_encode(getDataTemplate())) . // Same here.
    ",`status`=" . json_encode(json_encode($status)) . // And here too.
    " WHERE `id` = " . $id;

    if ($conn->query($query)) { // That's a big query
        $conn->close();
        // the account has been created, build & email the verification link
        $headers  = "From: Flare Bot <flarebot@flare-esports.net>\r\nTo: " . $email . "\r\n";
        $headers .= "MIME-Version: 1.0\r\nContent-Type: text/html; charset=iso-8859-1\r\n";
        $message = "<div style=\"background-color:#f2f2f2;\"><table width=\"100%\"\r\nstyle=\"background-color:#f2f2f2;padding:1.5em 0px 2em 0px;\r\nbox-shadow:inset #aaa 0px 0px 10px -2px;\"><tbody><tr><td>\r\n<table style=\"width:640px;padding:10px 25px;\r\nbackground-color:#fff;border-radius:5px;\r\nbox-shadow:#777 0px 1px 8px -2px;color:#000;\" align=\"center\"><tbody>\r\n<tr><td><h1 style=\"text-align:center;font-size:3em;\r\nfont-family:&quot;Verdana&quot;, sans-serif;font-weight:lighter;\">\r\nWelcome to Flare E-Sports!</h1><p style=\"font-size:2em;\r\nfont-family:&quot;Arial&quot;, sans-serif;\">\r\nHello ";
        $message .= htmlspecialchars($name);
        $message .= ",</p>\r\n<p style=\"font-size:1.5em;font-family:&quot;Arial&quot;, sans-serif;\r\ntext-indent:2em;text-align:justify\">You have successfully registered\r\nyour <a href=\"https://www.steamcommunity.com/profiles/";
        $message .= $steamID;
        $message .= "\"\r\nstyle=\"color:#FF9800;text-decoration:underline;\">Steam Account";
        $message .= "\r\n</a> with Flare E-Sports. If this is your account, please click the\r\nverification link below to complete the registration process. This\r\nlink will only work for the next 48 hours. </p><div\r\nstyle=\"margin:0 20px;word-break:break-all\"><a href=\"\r\nhttp://www.flare-esports.net/verifyEmail?u=";
        $message .= $steamID;
        $message .= '&k=';
        $message .= $status["link"]["secret"];
        $message .= "\r\n\" style=\"font-size: 1.2em;font-family: &quot;Consolas&quot;, monospace;\r\ncolor: #FF9800;\">\r\nhttp://www.flare-esports.net/verifyEmail?u=";
        $message .= $steamID;
        $message .= '&k=';
        $message .= $status["link"]["secret"];
        $message .= "\r\n</a></div><p style=\"font-size:1.5em;font-family:&quot;Arial&quot;,\r\nsans-serif;text-indent:2em;\">Thank you for joining Flare E-Sports, GL HF!\r\n</p></td></tr></tbody></table><p style=\"font-size:0.8em;font-family:\r\n&quot;Arial&quot;, sans-serif;color:#888;text-align:center;display:\r\nblock;margin:1em auto;width:600px;\">If this is not your account, or\r\nyou did not register this email, please click <a href=\"\r\nhttp://www.flare-esports.net/verifyEmail?cancel=yes&u=";
        $message .= $steamID;
        $message .= '&k=';
        $message .= $status["link"]["secret"];
        $message .= "\r\n\" style=\"color:#444;\">here</a> to cancel and we'll remove your\r\nemail from our system. If you have any questions or concerns, please\r\nvisit <a href=\"http://flare-esports.net/faq\" style=\"color:#444;\">\r\nflare-esports.net/faq</a> or contact us via email at\r\n<a href=\"mailto:support@flare-esports.net\">support@flare-esports.net</a>\r\n</p></td></tr></tbody></table></div>";
        mail($email, 'Welcome to Flare E-Sports, ' . $name . '!', $message, $headers);

        consoleExit("{\"success\":true,\"secret\":\"" . $secret . "\"}");

    } else {
        // Query failed, I cri evrytiem ;(
        error_log("13263 - The SQL query to complete user signup failed. Here's what we got: " . $conn->error_list);
        $conn->close();
        consoleExit("{\"success\":false,\"error\":\"13263\"}");
    }

} else {
    error_log("Missing \$email, \$name, or \$verify for included file \"makeAccount\"!");
    die("Missing email, name or verify!");
}


?>
