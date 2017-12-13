<?php

require("dbInf.php");

// Load the RSS feed into a JSON array-like object;
$rss = new DOMDocument();
$rss->load("http://blog.counter-strike.net/index.php/category/updates/feed/");

$feed = array();
foreach ($rss->getElementsByTagName("item") as $item){
    array_push($feed, array(
        "title" => $item->getElementsByTagName("title")->item(0)->nodeValue,
        "link" => $item->getElementsByTagName("link")->item(0)->nodeValue,
        "date" => strtotime($item->getElementsByTagName("pubDate")->item(0)->nodeValue),
        "content" => $item->getElementsByTagName("encoded")->item(0)->nodeValue
    ));
}
// Reverse so that oldest posts are checked first
$feed = array_reverse($feed);

// var_dump($feed);

$conn = mysqli_connect(DB_SERVER, USERNAME, PASSWORD, UPDATEDB);
if ($conn->connect_error){
    die("Connection failed");
}

foreach ($feed as $item){
    $query = "SELECT `id` FROM `csgo-updates` WHERE `link`='" . $item["link"] . "'";
    $result = $conn->query($query);
    if ($result->num_rows == 0){
        $query = "INSERT INTO `csgo-updates` (`title`, `date`, `link`, `content`) "
               . "VALUES (\""
               . $item["title"] . "\", \""
               . date("Y-m-d H:i:s", $item["date"]) . "\", \""
               . $item["link"] . "\", \""
               . $item["content"] . "\")";
        $conn->query($query);

        // Build Email
        $headers[] = "MIME-Version: 1.0";
        $headers[] = "Content-type: text/html; charset=iso-8859-1";
        $headers[] = "To: Dev Team <devteam@flare-esports.net>, "
                   . "Mick Ashton <mickxashton@gmail.com>";
        $headers[] = "From: Flare Bot <flarebot@flare-esports.net>";

        $message = '
<html>
<head>
    <title>' . $item["title"] . '</title>
    <style>
    body {
        background:#010713;
        color:#999b9d;
        margin:0;
        padding:0;
        font-family:Calibri, Candara, Segoe, Geneva, Tahoma, Arial, sans-serif;
        font-size:18px;
        line-height:145%;
    }
    .body {
        width: 100%;
        height: 100%;
        background: #010713;
    }
    #container {
        width:605px;
        margin-left:auto;
        margin-right:auto;
        color:#999b9d;
        border:0;
        border-spacing:0;
        font-size:18px;
        line-height:145%;
    }
    #content {
        background-image:url("http://media.steampowered.com/apps/csgo/blog/images/contentBkndTexture.jpg");
        background-repeat:repeat-y;
        border-bottom:4px solid #171b1e;
    }
    #shadow {
        width:605px;
        padding: 22px 0 58px 0;
        background-image:url("http://media.steampowered.com/apps/csgo/blog/images/main_blog_shadow.png");
        background-position:top;
        background-repeat:no-repeat;
    }
    #post {
        width:528px;
        margin:12px 0 0 43px;
        min-height:780px;
        padding:2px 34px 18px 0;
    }
    #post h2 {
        margin:0 0 5px 0;
        font-size:32px;
        line-height:36px;
        color:#97b7d1;
    }
    h2 a {
        color:#97b7d1 !important;
        text-decoration:none;
    }
    #post p{
        margin-top:10px;
        padding-bottom:10px;
    }
    .date {
        margin: 2px 0 10px 0 !important;
        font-size:14px;
        padding-top:5px;
    }
    </style>
</head>
<body>
<table class="body"><tr><td>
    <table id="container"><tr><td>
        <div id="content">
            <div id="shadow">
                <div id="post">
                    <p style="font-family:courier;
                              font-size:0.8em;
                              margin:0;
                              padding:0">
                        <i>This is an automatic message, please don\'t respond!</i></p>
                    <h2>
                        <a href="' . $item["link"] . '">' . $item["title"] . '</a>
                    </h2>
                    <p class="date">' . date("Y.m.d", $item["date"])
                              . ' - <img style="margin-bottom: -1px;"'
                              . ' src="http://media.steampowered.com/apps/csgo/blog/images/tags/cs_blog_tag.png" />
                    </p>
                    ' . $item["content"] . '
                </div>
            </div>
        </div>
    </td></tr></table>
</td></tr></table>
</body>
</html>';

        mail("devteam@flare-esports.net, mickxashton@gmail.com", // Receivers
             "[CSGO Update] " . $item["title"], // Subject
             $message, // Message
             implode("\r\n", $headers)); // Headers

        // Display for testing
        echo $message;

        // Break because the next time it runs it will start with the next post
        // This is just to prevent overloading the mail() function
        break;

    }
}

?>
