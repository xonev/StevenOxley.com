locals {
  website_prefix = "stevenoxley_com"
  edge_redirects_name = "${local.website_prefix}_edge_redirects"
  edge_redirects_source = "${path.module}/edge_redirects.js"
  edge_redirects_archive = "${path.module}/edge_redirects.zip"
}

resource "aws_cloudfront_function" "edge_redirects" {
  name = "edge_redirects"
  runtime = "cloudfront-js-2.0"
  publish = true
  code = file("${path.module}/edge_redirects.js")
}
