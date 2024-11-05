resource "aws_route53_record" "budget" {
  zone_id = aws_route53_zone.domain.zone_id
  name = "budget.${local.website_domain}"
  type = "CNAME"
  records = ["aquamarine-cat.pikapod.net"]
  ttl = 300
}
