resource "aws_route53_record" "mail" {
  zone_id = aws_route53_zone.domain.zone_id
  name = local.website_domain
  type = "MX"
  ttl = "300"
  records = ["1 SMTP.GOOGLE.COM."]
}
