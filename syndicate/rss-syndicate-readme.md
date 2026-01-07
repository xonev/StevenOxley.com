# RSS Social Media Syndication Script

Syndicate RSS/Atom feed posts to multiple social media platforms with Babashka.

## Prerequisites

Install Babashka (v1.0.0 or higher): https://github.com/babashka/babashka#installation

Required Babashka features (included by default):
- `babashka.http-client` - HTTP requests
- `babashka.http-server` - OAuth callback server
- `clojure.java.browse` - Open browser for OAuth
- `cheshire.core` - JSON parsing
- `clojure.data.xml` - XML/RSS parsing

## Installation

1. Save the script as `rss-syndicate.clj`
2. Make it executable: `chmod +x rss-syndicate.clj`
3. Run setup: `bb rss-syndicate.clj setup`

## Usage

```bash
# Syndicate to all platforms
bb rss-syndicate.clj https://example.com/feed.xml

# Syndicate to specific platforms
bb rss-syndicate.clj https://example.com/feed.xml x bluesky mastodon

# Show setup instructions
bb rss-syndicate.clj setup
```

## Configuration

Create `~/.rss-syndicate-config.edn`:

```clojure
{:x {:api-key "your-api-key"
     :api-secret "your-api-secret"}
 :bluesky {:handle "yourhandle.bsky.social"
           :password "app-specific-password"}
 :mastodon {:instance "https://mastodon.social"
            :access-token "your-access-token"}
 :facebook {:page-id "your-page-id"
            :page-access-token "your-page-token"}
 :linkedin {:client-id "your-client-id"
            :client-secret "your-client-secret"}
 :instagram {:account-id "your-instagram-business-account-id"
             :access-token "your-access-token"}}
```

## Platform Setup

### X (Twitter)
- **Cost**: $100/month minimum for API access
- **Authentication**: OAuth 1.0a (User Context)
- **Steps**:
  1. Visit https://developer.twitter.com/
  2. Create project and app
  3. Go to "User authentication settings"
  4. Enable OAuth 1.0a
  5. Set app permissions to "Read and Write"
  6. Generate all 4 tokens:
     - API Key (Consumer Key)
     - API Secret (Consumer Secret)
     - Access Token
     - Access Token Secret
  7. Add all 4 tokens to config file

### Bluesky
- **Cost**: Free
- **Steps**:
  1. Go to Settings > Advanced > App Passwords
  2. Create new app password
  3. Add handle and app password to config

### Mastodon
- **Cost**: Free
- **Steps**:
  1. Go to Settings > Development on your instance
  2. Create new application with read/write permissions
  3. Copy access token to config

### LinkedIn
- **Cost**: Free (with limits)
- **Note**: Requires OAuth 2.0 flow - manual posting recommended

### Facebook
- **Cost**: Free
- **Steps**:
  1. Create app at https://developers.facebook.com/
  2. Add Facebook Login and Pages API
  3. Get long-lived Page Access Token
  4. Add page ID and token to config

### Instagram
- **Cost**: Free (through Facebook)
- **Requirements**:
  - Business/Creator account
  - Linked to Facebook Page
  - Media (image/video) required for posts
- **Note**: Complex setup through Facebook Graph API

## Features

- **Automatic Threading**: Splits long posts into threads for X (280 chars), Bluesky (300 chars), and Mastodon (500 chars)
- **HTML Stripping**: Cleans RSS content of HTML tags
- **Media Detection**: Identifies media enclosures in RSS feeds
- **Draft Preview**: Shows formatted post before publishing
- **Manual Approval**: Requires confirmation before posting to each platform
- **Link Back**: Includes original post link on all platforms

## Character Limits

- X: 280 characters
- Bluesky: 300 characters
- Mastodon: 500 characters
- LinkedIn: 3,000 characters
- Facebook: 63,206 characters
- Instagram: 2,200 characters (caption)

## Notes

- Instagram requires images/videos - text-only posts will error
- LinkedIn implementation requires OAuth web flow (not included)
- X API now requires paid access ($100/month minimum)
- Threading adds "[→]" marker to indicate continuation
- Configuration stored in `~/.rss-syndicate-config.edn`

## Error Handling

The script will:
- Skip platforms without configuration
- Show error for Instagram posts without media
- Display clear error messages for API failures
- Continue to other platforms if one fails

## Security

- Store credentials in `~/.rss-syndicate-config.edn` with appropriate permissions
- Use app-specific passwords where available (Bluesky)
- Consider using environment variables for sensitive tokens
- Never commit config file to version control
