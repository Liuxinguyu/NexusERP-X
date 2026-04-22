/**
 * 与后端 @PreAuthorize("@ss.hasPermi('...')") 对齐的权限标识（示例，可按后端实际调整）。
 */
export const SYSTEM_PERMS = {
  user: {
    query: 'system:user:query',
    add: 'system:user:add',
    edit: 'system:user:edit',
    remove: 'system:user:remove',
    resetPwd: 'system:user:resetPwd',
  },
  role: {
    query: 'system:role:query',
    add: 'system:role:add',
    edit: 'system:role:edit',
    remove: 'system:role:remove',
  },
  dict: {
    query: 'system:dict:query',
    add: 'system:dict:add',
    edit: 'system:dict:edit',
    remove: 'system:dict:remove',
  },
  shop: {
    query: 'system:shop:query',
    add: 'system:shop:add',
    edit: 'system:shop:edit',
    remove: 'system:shop:remove',
  },
  menu: {
    query: 'system:menu:query',
    add: 'system:menu:add',
    edit: 'system:menu:edit',
    remove: 'system:menu:remove',
  },
  org: {
    query: 'system:org:query',
    add: 'system:org:add',
    edit: 'system:org:edit',
    remove: 'system:org:remove',
  },
  config: {
    query: 'system:config:query',
    add: 'system:config:add',
    edit: 'system:config:edit',
    remove: 'system:config:remove',
  },
  post: {
    query: 'system:post:query',
    add: 'system:post:add',
    edit: 'system:post:edit',
    remove: 'system:post:remove',
  },
  monitor: {
    loginlog: 'monitor:loginlog:list',
    loginlogRemove: 'monitor:loginlog:remove',
    operlog: 'monitor:operlog:list',
    operlogRemove: 'monitor:operlog:remove',
    online: 'monitor:online:list',
    onlineKick: 'monitor:online:forceLogout',
  },
} as const
