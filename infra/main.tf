locals {
  website_origin_id = "stevenoxley-com-site-origin"
  website_domain = "stevenoxley.com"
  website_bucket_name = "stevenoxley-com-site"
}

resource "aws_route53_zone" "domain" {
  name = local.website_domain
}

resource "aws_s3_bucket" "website" {
  bucket = local.website_bucket_name
}

resource "aws_route53_record" "www_domain" {
  zone_id = aws_route53_zone.domain.zone_id
  name = "www.${local.website_domain}"
  type = "A"

  alias {
    name = aws_cloudfront_distribution.website.domain_name
    zone_id = aws_cloudfront_distribution.website.hosted_zone_id
    evaluate_target_health = true
  }
}

resource "aws_route53_record" "domain" {
  zone_id = aws_route53_zone.domain.zone_id
  name = local.website_domain
  type = "A"

  alias {
    name = aws_route53_record.www_domain.name
    zone_id = aws_route53_record.www_domain.zone_id
    evaluate_target_health = true
  }
}

resource "aws_route53_record" "bluesky_verification" {
  zone_id = aws_route53_zone.domain.zone_id
  name = "_atproto.${local.website_domain}"
  type = "TXT"
  ttl = 300
  records = ["did=did:plc:lnmfyhng3xfs4q2nv5bsdwpo"]
}

resource "aws_cloudfront_distribution" "website" {
  origin {
    domain_name = aws_s3_bucket.website.bucket_regional_domain_name
    origin_id = local.website_origin_id
    origin_access_control_id = aws_cloudfront_origin_access_control.website.id
  }

  aliases = [local.website_domain, "www.${local.website_domain}"]
  default_root_object = "index.html"
  enabled = true
  http_version = "http2and3"
  is_ipv6_enabled = true

  # North America and Europe only
  price_class = "PriceClass_100"

  restrictions {
    geo_restriction {
      locations = []
      restriction_type = "none"
    }
  }

  viewer_certificate {
    acm_certificate_arn = aws_acm_certificate.website.arn
    ssl_support_method = "sni-only"
    minimum_protocol_version = "TLSv1.2_2021"
  }

  default_cache_behavior {
    allowed_methods = ["GET", "HEAD"]
    cached_methods = ["GET", "HEAD"]
    target_origin_id = local.website_origin_id

    forwarded_values {
      query_string = false

      cookies {
        forward = "none"
      }
    }

    function_association {
      event_type = "viewer-request"
      function_arn = aws_cloudfront_function.edge_redirects.arn
    }

    viewer_protocol_policy = "redirect-to-https"
  }

  custom_error_response {
    error_code = 403
    response_code = 404
    response_page_path = "/404/index.html"
  }
}

resource "aws_acm_certificate" "website" {
  provider = aws.use1
  domain_name = local.website_domain
  validation_method = "DNS"
  subject_alternative_names = ["www.${local.website_domain}"]

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_route53_record" "website_domain_validations" {
  for_each = {
    for dvo in aws_acm_certificate.website.domain_validation_options : dvo.domain_name => {
      name   = dvo.resource_record_name
      record = dvo.resource_record_value
      type   = dvo.resource_record_type
    }
  }

  allow_overwrite = true
  name            = each.value.name
  records         = [each.value.record]
  ttl             = 60
  type            = each.value.type
  zone_id         = aws_route53_zone.domain.zone_id
}

resource "aws_acm_certificate_validation" "website" {
  provider = aws.use1
  certificate_arn         = aws_acm_certificate.website.arn
  validation_record_fqdns = [for record in aws_route53_record.website_domain_validations : record.fqdn]
}

resource "aws_s3_bucket_policy" "website_cloudfront_access" {
  bucket = aws_s3_bucket.website.id
  policy = data.aws_iam_policy_document.allow_access_from_cloudfront.json
}

data "aws_iam_policy_document" "allow_access_from_cloudfront" {
  policy_id = "PolicyForCloudFrontPrivateContent"
  statement {
    sid = "AllowCloudFrontServicePrincipal"
    principals {
      type = "Service"
      identifiers = ["cloudfront.amazonaws.com"]
    }
    actions = ["s3:GetObject"]
    resources = [
      aws_s3_bucket.website.arn,
      "${aws_s3_bucket.website.arn}/*"
    ]
    condition {
      test = "StringEquals"
      variable = "AWS:SourceArn"
      values = [aws_cloudfront_distribution.website.arn]
    }
  }
}

resource "aws_cloudfront_origin_access_control" "website" {
  name = "stevenoxley-com-site"
  origin_access_control_origin_type = "s3"
  signing_behavior = "always"
  signing_protocol = "sigv4"
}
