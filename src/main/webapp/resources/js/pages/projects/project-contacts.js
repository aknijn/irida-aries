/**
 * @file This file is used by both project > users and project > user groups
 * for handling user / group project roles.
 */
import $ from "jquery";
import "./../../vendor/plugins/jquery/select2";
import "./../../vendor/datatables/datatables";
import {
  generateColumnOrderInfo,
  tableConfig
} from "../../utilities/datatables-utilities";
import { formatDate } from "../../utilities/date-utilities";
import { showNotification } from "../../modules/notifications";

const $table = $("#usersTable");
const canManage = !!$table.data("admin");

/*
Get the template for the role wrapper.  This is used to populate the member role column.
If the user is a manager or administrator, the template will contain a select input,
if the user is a collaborator, the template will have just the labels for the roles.
 */
const roleTemplateWrapper = document.querySelector("#role-template-wrapper");

/*
Get the current order of the columns with there label format.
 */
const COLUMNS = generateColumnOrderInfo();

/*
Create a custom DataTables configuration.
 */
const CONFIG = Object.assign({}, tableConfig, {
  ajax: $table.data("url"),
  columnDefs: [
    {
      targets: [COLUMNS.USER_USERNAME],
      render(data, type, full) {
        return `${full.name}`;
      }
    },
    {
      targets: [COLUMNS.USER_EMAIL],
      render(data, type, full) {
        return `${full.email}`;
      }
    }
  ]
});

/*
Initialize the DataTable
 */
const $dt = $table.DataTable(CONFIG);
