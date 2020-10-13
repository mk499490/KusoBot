package com.github.mk499490.kusobot;

import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.server.ExportException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class KusoBot {
    public static void main(String[] args) {
        /*
            やること
            ●辞書ファイル読み込み
            ・txtファイルかcsvファイルにログ出力(ツイート日、ツイートする単語の行、結果(成功、成功(アルファベット→カタカナ)、失敗、スルー)、ツイートID)
            ●最後にツイートした言葉をpropertiesファイルかなにかに記録(行番号だけ)
            ・APIキーとかをpropertiesファイルに記録(setup引数つけたら設定できる)
            ・単語重複回避(一つ上の言葉とかぶってたらスルー)
            ・英単語はカタカナで(E列がアルファベットならA列のをツイート)
         */

        /* if (args.length < 1) {
            System.out.println("Usage: java twitter4j.examples.tweets.UpdateStatus [text]");
            System.exit(-1);
        } */

        //ログイン処理
        try {
            Twitter twitter = new TwitterFactory().getInstance();
            try {
                // get request token.
                // this will throw IllegalStateException if access token is already available
                RequestToken requestToken = twitter.getOAuthRequestToken();
                System.out.println("Got request token.");
                System.out.println("Request token: " + requestToken.getToken());
                System.out.println("Request token secret: " + requestToken.getTokenSecret());
                AccessToken accessToken = null;

                BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
                while (null == accessToken) {
                    System.out.println("Open the following URL and grant access to your account:");
                    System.out.println(requestToken.getAuthorizationURL());
                    System.out.print("Enter the PIN(if available) and hit enter after you granted access.[PIN]:");
                    String pin = br.readLine();
                    try {
                        if (pin.length() > 0) {
                            accessToken = twitter.getOAuthAccessToken(requestToken, pin);
                        } else {
                            accessToken = twitter.getOAuthAccessToken(requestToken);
                        }
                    } catch (TwitterException te) {
                        if (401 == te.getStatusCode()) {
                            System.out.println("Unable to get the access token.");
                        } else {
                            te.printStackTrace();
                        }
                    }
                }
                System.out.println("Got access token.");
                System.out.println("Access token: " + accessToken.getToken());
                System.out.println("Access token secret: " + accessToken.getTokenSecret());
            } catch (IllegalStateException ie) {
                // access token is already available, or consumer key/secret is not set.
                if (!twitter.getAuthorization().isEnabled()) {
                    System.out.println("OAuth consumer key/secret is not set.");
                    System.exit(-1);
                }
            }

            // ツイート済み単語の行数取得
            Properties properties = new Properties();
            try {
                properties.load(Files.newBufferedReader(Paths.get(System.getProperty("user.dir"),"lastword.properties"), StandardCharsets.UTF_8));
            } catch (IOException e) {
                System.err.println("Failed to load lastword.properties!");
                e.printStackTrace();
                System.exit(-1);
            }

            int lastWord = Integer.valueOf(properties.getProperty("lastword", "-1"));

            // CSV取得
            FileInputStream fi = null;
            InputStreamReader is = null;
            BufferedReader br = null;

            List<String> words = new ArrayList<String>();

            int i = 0;

            try {
                fi = new FileInputStream(System.getProperty("user.dir") + File.separator + "words.csv");
                is = new InputStreamReader(fi);
                br = new BufferedReader(is);

                String line;

                while((line = br.readLine()) != null) {
                    words.add(line);
                    System.out.println(line);
                    i++;
                }
            } catch (Exception e) {
                System.err.println("Failed to load words.csv!");
                e.printStackTrace();
                System.exit(-1);
            } finally {
                try {
                    br.close();
                } catch (Exception e) {
                    System.err.println("Failed to unload words.csv!");
                    e.printStackTrace();
                    System.exit(-1);
                }
            }

            // 単語選定
            String[] tweetWord = new String[2];
            tweetWord = words.get(lastWord + 1).split(",");
            System.out.println("Tweeted word will be: " + tweetWord[1]);
            String word = tweetWord[1];

            // ツイート
            Status status = twitter.updateStatus("test" + word);
            System.out.println("Successfully updated the status to [" + status.getText() + "].");

            //propertiesのlastword書き換え
            try (
                    FileOutputStream f = new FileOutputStream(System.getProperty("user.dir") + File.separator + "lastword.properties");
                    BufferedOutputStream b = new BufferedOutputStream(f);
            ){
                properties.setProperty("lastword", String.valueOf(lastWord + 1));
                properties.store(b, "Latest tweeted words (Make sure that FIRST LINE IS 0!! If you haven't tweeted anything, set -1)");
            } catch (IOException e) {
                System.err.println("Failed to rewrite lastword.properties!");
                e.printStackTrace();
            }

        } catch (TwitterException te) {
            te.printStackTrace();
            System.out.println("Failed to get timeline: " + te.getMessage());
            System.exit(-1);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            System.out.println("Failed to read the system input.");
            System.exit(-1);
        }

    }
}
