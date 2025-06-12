# 阶段一：构建应用
FROM node:22-alpine AS build

# 设置环境变量强制使用JS版本的Rollup
ENV ROLLUP_USE_NATIVE=false

WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build

# 阶段二：服务应用
FROM nginx:alpine3.20-slim
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]