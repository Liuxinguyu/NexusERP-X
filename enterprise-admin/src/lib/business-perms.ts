export const ERP_PERMS = {
  saleOrder: {
    list: 'erp:sale-order:list',
    add: 'erp:sale-order:add',
    submit: 'erp:sale-order:submit',
    approve: 'erp:sale-order:approve',
    reject: 'erp:sale-order:reject',
    outbound: 'erp:sale-order:outbound',
    remove: 'erp:sale-order:remove',
  },
  purchaseOrder: {
    list: 'erp:purchase-order:list',
    add: 'erp:purchase-order:add',
    submit: 'erp:purchase-order:submit',
    approve: 'erp:purchase-order:approve',
    reject: 'erp:purchase-order:reject',
    inbound: 'erp:purchase-order:inbound',
    remove: 'erp:purchase-order:remove',
  },
  receivable: {
    list: 'erp:receivable:list',
    record: 'erp:receivable:record',
  },
  payable: {
    list: 'erp:payable:list',
    record: 'erp:payable:record',
  },
  report: {
    view: 'erp:report:view',
  },
  stock: {
    list: 'erp:stock:list',
  },
} as const

export const OA_PERMS = {
  attendance: {
    view: 'oa:attendance:list',
  },
  approval: {
    list: 'oa:approval:list',
    approve: 'oa:approval:approve',
  },
  task: {
    list: 'oa:task:list',
    add: 'oa:task:add',
    edit: 'oa:task:edit',
    delete: 'oa:task:delete',
  },
  cloudDisk: {
    list: 'oa:cloud-disk:list',
    create: 'oa:file:create-folder',
    upload: 'oa:file:upload',
    delete: 'oa:file:delete',
  },
  notice: {
    list: 'oa:notice:list',
    add: 'oa:notice:add',
    edit: 'oa:notice:edit',
    publish: 'oa:notice:publish',
    remove: 'oa:notice:remove',
  },
} as const

export const WAGE_PERMS = {
  slip: {
    list: 'wage:slip:list',
    generate: 'wage:slip:generate',
    edit: 'wage:slip:edit',
    confirm: 'wage:slip:confirm',
  },
  config: {
    list: 'wage:item:list',
  },
  itemConfig: {
    list: 'wage:item:list',
    add: 'wage:item:add',
    edit: 'wage:item:edit',
    delete: 'wage:item:delete',
  },
} as const

export const CRM_PERMS = {
  opportunity: {
    list: 'erp:opportunity:list',
    add: 'erp:opportunity:add',
    edit: 'erp:opportunity:edit',
    remove: 'erp:opportunity:remove',
    advance: 'erp:opportunity:advance',
  },
  contract: {
    list: 'erp:contract:list',
    add: 'erp:contract:add',
    edit: 'erp:contract:edit',
    remove: 'erp:contract:remove',
  },
} as const
