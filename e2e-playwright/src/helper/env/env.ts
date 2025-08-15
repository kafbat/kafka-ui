import * as dotenv from 'dotenv';
import path from 'path';

const env = process.env.ENV;

if (env) {
  const fullPath = path.resolve(`src/helper/env/.env.${env}`);
  const result = dotenv.config({
    override: true,
    path: fullPath,
  });

  if (result.error) {
    console.error(`Failed to load .env.${env} from ${fullPath}:`, result.error);
  } else {
    console.log(`Loaded environment variables from .env.${env}`);
  }
} else {
  console.error("ENV not provided. Use ENV=dev|qa|prod in your npm script.");
}
