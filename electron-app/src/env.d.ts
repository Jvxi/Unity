/// <reference types="vite/client" />
declare module 'sockjs-client/dist/sockjs' {
  const SockJS: any;
  export default SockJS;
}
declare module 'animejs' {
  export function animate(targets: any, options: any): any;
  export function stagger(val: number, opts?: any): any;
}
declare module 'element-plus/dist/locale/zh-cn.mjs' {
  const zhCn: any;
  export default zhCn;
}