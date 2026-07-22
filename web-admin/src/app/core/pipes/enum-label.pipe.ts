import { Pipe, PipeTransform } from '@angular/core';
import { Priority, WorkOrderStatus } from '../models/work-order.model';
import { priorityLabel, statusLabel } from '../utils/labels';

@Pipe({ name: 'priorityLabel', standalone: true })
export class PriorityLabelPipe implements PipeTransform {
  transform(value: Priority): string {
    return priorityLabel(value);
  }
}

@Pipe({ name: 'statusLabel', standalone: true })
export class StatusLabelPipe implements PipeTransform {
  transform(value: WorkOrderStatus): string {
    return statusLabel(value);
  }
}
