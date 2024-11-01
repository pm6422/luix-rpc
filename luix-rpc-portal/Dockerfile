FROM nginx:alpine3.20-slim

# Delete all files in nginx default directory
RUN rm /usr/share/nginx/html/*

# Copy all files in /app to /usr/share/nginx/html
COPY app /usr/share/nginx/html/

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
