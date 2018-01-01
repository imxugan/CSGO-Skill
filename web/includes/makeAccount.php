<?php

require_once("setup.php");
require_once("dbInf.php");
require_once("checkSteamProfile.php");

if (!isset($app)) {
    $app = false;
}

/**
 * This file must be included at the time when a new account should be built
 * and only takes two external variables: $steamID, $create [OPTIONAL]
 *
 * If `isset($create) == true`, we also expect to have an $email and $name.
 * Otherwise, we simply check that the account COULD be created.
 */

if (!isset($steamID)) {
    error_log("Missing \$steamID for included file \"makeAccount\"!");
    die("Missing steamID!");
}

if (!isset($create)) { // Reserve Row, return Verify key

    // Would terminate script if it fails
    check($steamID, true);

    $conn = mysqli_connect(DB_SERVER, USERNAME, PASSWORD, FLAREDB);
    if ($conn->connect_error) {
        error_log("1312 - Failed to connect to MySQL Database '" . FLAREDB .
        "' with error (" . $conn->connect_errno . "): " . $conn->connect_error);
        consoleExit("{\"success\":false,\"error\":\"1312\"}");
    }

    $steamID = $conn->real_escape_string($steamID);

    // Look up accounts which are registered.
    $query = "SELECT `id` FROM `Players_01` WHERE `steamid`=\"" . $steamID .
    "\" AND `verified` = 1 AND NOT `secret`=\"empty\"";
    $result = $conn->query($query);

    if ($result->num_rows !== 0) {
        // Steam ID is already in use, cannot create profile.
        $conn->close();
        consoleExit("{\"success\":false,\"error\":\"13241\"}");
    }

    // Check for existing, non-registered accounts
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
            error_log("13261 - The SQL query to delete a reserved account row failed. Here's what we got: " . print_r($conn->error_list, true));
            $conn->close();
            consoleExit("{\"success\":false,\"error\":\"13261\"}");
        }
    }

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
             "\", " . jsonToSql($status) . ")";
    if ($conn->query($query)) {
        $conn->close();
        consoleExit("{\"success\":true,\"steamid\":\"" . $steamID . "\",\"verify\":\"" . $string . "\"}");
    } else {
        error_log("13262 - The SQL query to reserve user account failed. Here's what we got: " . print_r($conn->error_list, true));
        $conn->close();
        consoleExit("{\"success\":false,\"error\":\"13262\"}");
    }

} else if (isset($email) && isset($name) && isset($verify)){
    // Finalize & create account

    $conn = mysqli_connect(DB_SERVER, USERNAME, PASSWORD, FLAREDB);
    if ($conn->connect_error) {
        error_log("1312 - Failed to connect to MySQL Database '" . FLAREDB .
        "' with error (" . $conn->connect_errno . "): " . $conn->connect_error);
        exit("{\"success\":false,\"error\":\"1312\"}");
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
        exit("{\"success\":false,\"error\":\"TRY AGAIN\"}");
    }

    // Grab `id` and `status` for things
    $result = $result->fetch_assoc();
    $id = $result["id"];
    $status = json_decode($result["status"]);

    if ($status->verify !== $verify) {
        // Given $verify doesn't match. Cannot validate request!
        $conn->close();
        exit("{\"success\":false,\"error\":\"1323\"}");
    }

    unset($status->verify); // No longer needed
    // Just to clarify, although this is unset here, the database will not be
    // updated unless the provided info is good and the account created.

    $name = $conn->real_escape_string(substr($name, 0, 30));

    // Check that name is at least 3 chars, and email is 5-50 chars
    //             (a@b.c - anextremelylongemailthatnooneuses@bigdomain.domain)
    if (strlen($name) < 3 || strlen($email) < 5 || strlen($email) > 50){
        $conn->close();
        exit("{\"success\":false,\"error\":\"1325\"}");
    }

    // Check that email does not contain strange non-email characters
    $invalids = str_split("'\"/;:[]{}~+=!#$%^&*()");
    foreach ($invalids as $char){
        if (strpos($email, $char) !== false){
            $conn->close();
            exit("{\"success\":false,\"error\":\"1325\"}");
        }
    }

    // Validity check
    $email = filter_var($email, FILTER_SANITIZE_EMAIL);
    if (!filter_var($email, FILTER_VALIDATE_EMAIL)){
        $conn->close();
        exit("{\"success\":false,\"error\":\"1325\"}");
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
        exit("{\"success\":false,\"error\":\"13242\"}");
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
        exit("{\"success\":false,\"error\":\"13243\"}");
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

    $stats = check($steamID); // Double check the account
    if ($stats === NULL || $stats === false ) {
        // Account is no longer valid (for some reason), I cry everytime :'(
        $conn->close();
        exit("{\"success\":false,\"error\":\"13223\"}");
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
    $status->{"link"} = array(
        "secret" => substr(hash("sha256", random_bytes(200)), 0, 20),
        "action" => "verify"
    );

    // Grab the Data template
    require_once("data_template.php");

    $query = "UPDATE `Players_01` SET " .
    "`username`=\"" . $name .
    "\",`persona`=\"" . $name .
    "\",`email`=\"" . $email .
    "\",`secret`=\"" . $secret .
    "\",`current`=" . jsonToSql($stats) .
    ",`data`=" . jsonToSql(getDataTemplate()) .
    ",`status`=" . jsonToSql($status) .
    " WHERE `id` = " . $id;

    if ($conn->query($query)) { // That's a big query
        $conn->close();
        // the account has been created, build & email the verification link
        $headers  = "From: Skill Bot <skillbot@csgo-skill.com>\r\nTo: " . $email . "\r\n";
        $headers .= "MIME-Version: 1.0\r\nContent-Type: text/html; charset=iso-8859-1\r\n";
        $message = '<div style="background-color:#f2f2f2;"><table width="100%" style="background-color:#f2f2f2;padding:1.5em 0px 2em 0px;box-shadow:inset #aaa 0px 0px 10px -2px;"><tbody><tr><td><table style="width:640px;padding:10px 25px;background-color:#fff;border-radius:5px;box-shadow:#777 0px 1px 8px -2px;color:#000;" align="center"><tbody><tr><td><h1 style="text-align:center;font-size:3em;font-family:&quot;Verdana&quot;, sans-serif;font-weight:lighter;">Welcome to CSGO Skill!</h1><p style="font-size:2em;font-family:&quot;Arial&quot;, sans-serif;">Hello ';
        $message .= htmlspecialchars($name);
        $message .= ',</p><p style="font-size:1.5em;font-family:&quot;Arial&quot;, sans-serif;text-indent:2em;text-align:justify">You have successfully registered your <a href="https://steamcommunity.com/profiles/';
        $message .= $steamID;
        $message .= '" style="color:#FF9800;text-decoration:underline;">Steam Account</a> with CSGO Skill, a free application provided by Flare E-Sports. If this is your account, please click the verification link below to complete the registration process. This link will only work for the next 48 hours.</p><div style="margin:0 20px;word-break:break-all"><a href="http://csgo-skill.com/verifyEmail?u=';
        $message .= $steamID;
        $message .= '&k=';
        $message .= $status->link->secret;
        $message .= '" style="font-size: 1.2em;font-family: &quot;Consolas&quot;, monospace;color: #FF9800;">http://csgo-skill.com/verifyEmail?u=';
        $message .= $steamID;
        $message .= '&k=';
        $message .= $status->link->secret;
        $message .= '</a></div><p style="font-size:1.5em;font-family:&quot;Arial&quot;,sans-serif;text-indent:2em;">Thank you for joining CSGO Skill, GL HF!</p></td></tr></tbody></table><p style="font-size:0.8em;font-family:&quot;Arial&quot;, sans-serif;color:#888;text-align:center;display:block;margin:1em auto;width:600px;">If this is not your account, or you did not register this email, please ignore this email. We will not continue to email you. If you have any questions or concerns, please visit <a href="http://csgo-skill.com/about" style="color:#444;">csgo-skill.com/about</a> or contact us via email at<a href="mailto:support@flare-esports.net">support@flare-esports.net</a></p></td></tr></tbody></table></div>';
        mail($email, 'Welcome to CSGO Skill, ' . $name . '!', $message, $headers);
    } else {
        // Query failed, I cri evrytiem ;(
        error_log("13263 - The SQL query to complete user signup failed. Here's what we got: " . print_r($conn->error_list, true));
        $conn->close();
        exit("{\"success\":false,\"error\":\"13263\"}");
    }

    // Return to `addAccount` flow

} else {
    error_log("Missing \$email, \$name, or \$verify for included file \"makeAccount\"!");
    die("Missing email, name or verify!");
}


?>
