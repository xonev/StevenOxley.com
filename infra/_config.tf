terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "5.71.0"
    }
    archive = {
      source  = "hashicorp/archive"
      version = "2.6.0"
    }
  }
  backend "s3" {
    role_arn = "arn:aws:iam::713881783677:role/OrganizationAccountAccessRole"
    session_name = "terraform-state-usw2"

    bucket         = "stevenoxley-personal-prod-state"
    key            = "stevenoxley-com-website.tfstate"
    region         = "us-west-2"
    dynamodb_table = "stevenoxley-personal-prod-state"
  }
}

provider "aws" {
  region = "us-west-2"
  assume_role {
    role_arn = "arn:aws:iam::713881783677:role/OrganizationAccountAccessRole"
    session_name = "terraform-prod-usw2"
  }

  default_tags {
    tags = {
      project = "stevenoxley-com-website"
      environment = "prod"
    }
  }
}

provider "aws" {
  alias = "use1"
  region = "us-east-1"
  assume_role {
    role_arn = "arn:aws:iam::713881783677:role/OrganizationAccountAccessRole"
    session_name = "terraform-prod-use1"
  }

  default_tags {
    tags = {
      project = "stevenoxley-com-website"
      environment = "prod"
    }
  }
}
